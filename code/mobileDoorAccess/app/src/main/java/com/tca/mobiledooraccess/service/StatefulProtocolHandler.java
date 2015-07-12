package com.tca.mobiledooraccess.service;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Base64;
import android.util.Log;

import com.tca.mobiledooraccess.MainActivity;
import com.tca.mobiledooraccess.R;
import com.tca.mobiledooraccess.UnlockProgressActivity;
import com.tca.mobiledooraccess.utils.RSACrypto;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

/**
 * State machine for NFC high-level logic and protocol
 */
public final class StatefulProtocolHandler extends BaseMsgHandler {
    private static final String TAG = "StmProtocolHandler";
    /**
     * NFC protocol states for communication with CardEmulationService.
     */
    public final static int MSG_NFC_CONNECTION_LOST = 0;
    public final static int MSG_NFC_NEW_TERMINAL = 1;
    public final static int MSG_NFC_RECEIVED_PACKET = 2;
    public final static int MSG_NFC_SEND_PACKET = 3;
    // Return-channel: messages targeting CardEmulationService
    // This typically is outgoing data: Android phone --> terminal
    private Messenger mNfcCardEmulationService;

    /**
     * State machine for the "high-level" protocol.
     * States emulate the progress in the ordered message exchange defined by the protocol.
     * Includes type of all possible states and the next state.
     */
//    public enum ProtocolStep {
//        PROTO_MSG0_TERMINAL_CERTIFICATE_HASH,
//        PROTO_MSG1_TUM_ID_AND_NONCE,
//        PROTO_MSG2_RECEIVE_NONCE,
//        PROTO_MSG3_SEND_TOKEN_AND_NONCE
//    }
    public final static int PROTO_MSG0_TERMINAL_CERTIFICATE_HASH = 0;
    public final static int PROTO_MSG1_TUM_ID_AND_NONCE = 1;
    public final static int PROTO_MSG2_RECEIVE_NONCE = 2;
    public final static int PROTO_MSG3_SEND_TOKEN_AND_NONCE = 3;
    private int stmNextState = 0;
    // Protocol nonce
    private String r_S = "";    //< Random nonce r_S
    private String r_T = "";    //< Received nonce r_T from terminal
    // Public / Private Key Cryptography
    private RSACrypto crypto;

    /**
     * Progress states for broadcasting the current progress in the NFC protocol.
     */
    public final static String INTENT_PROTOCOL_PROGRESS = "tumgetin_nfc_protocol_progress";


    public StatefulProtocolHandler() {
        super();
    }

    /**
     * NFC state or incoming message aggregated by the CardEmulationService.
     * Message direction: terminal --> Android phone
     * @param msg   State or aggregated message received from the terminal.
     */
    @Override
    public void handleMessage(Message msg) {
        Log.d(TAG, "[Thread" + Thread.currentThread().getName() + "] Got message with what " + msg.what);

        switch (msg.what) {
            /**
             * Initial message on first contact.
             */
            case MSG_NFC_NEW_TERMINAL: {
                // Always take new messenger because CardEmulation's manager is recreated each time
                // it binds to a new terminal.
                mNfcCardEmulationService = msg.replyTo;

                // Reset this state machine to the initial protocol step 1
                // and reset stored nonces
                resetStates();
                initCryptoOnce();

                // Inform UI about new initial connection
                broadcastProtocolProgress(PROTO_MSG1_TUM_ID_AND_NONCE);

                /**
                 * Protocol step 1
                 * Send {r_S, pseudo_student_id} encrypted with T_pub key
                 */
                byte[] rawMsg = sendStudentIdAndNonce();
                // Encrypt message with terminal's public key
                byte[] encryptedMsg = crypto.encryptPlaintext(rawMsg);
                // Submit encrypted message to NFC card emulation service
                Message responseMsg = Message.obtain(null, MSG_NFC_SEND_PACKET);
                responseMsg.obj = encryptedMsg;
                try {
                    mNfcCardEmulationService.send(responseMsg);
                } catch (RemoteException e) {
                    Log.e("TAG", "Response not sent; RemoteException calling into " +
                            "CardEmulationService.");
                }
                // Go to step 2 in state machine: receiving a message from the terminal
                stmNextState = PROTO_MSG2_RECEIVE_NONCE;
            }
            break;

            /**
             * Incoming follow-up packets sent by the NFC terminal.
             */
            case MSG_NFC_RECEIVED_PACKET: {
                if (mNfcCardEmulationService == null) mNfcCardEmulationService = msg.replyTo;

                byte[] dataIn = (byte[])msg.obj;
                Log.i(TAG, "Complete packet received [" + dataIn.length + " bytes]\n");
                String msgIn = "";
                try {
                    // Decrypt message
                    byte[] rawMsgIn = crypto.decryptCiphertext(dataIn);
                    // Todo: if error: shutdown and cleanup
                    msgIn = new String(rawMsgIn, "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                Log.i(TAG, "Decrypted msg: " + msgIn);

                byte[] rawMsg = null;
                switch (stmNextState) {
                    case PROTO_MSG2_RECEIVE_NONCE: {
                        /**
                         * Protocol step 2 and step 3
                         * 2. Receive {r_s, r_t, T} encrypted with our S_pub key
                         * 3. Send {r_t, commands} encrypted with T_pub key
                         */
                        Log.d(TAG, "Current protocol step: PROTO_MSG2_RECEIVE_NONCE");
                        // Inform UI about entering step 2
                        broadcastProtocolProgress(stmNextState);
                        // TODO: add try block
                        rawMsg = checkResponseFromTerminal(msgIn);

                        // Last step in state machine already reached
                        stmNextState = PROTO_MSG3_SEND_TOKEN_AND_NONCE;
                    }
                    break;
                    // Todo: default action: reset state machine in case of unexpected error
                }
                // Inform UI about success in last step
                // Note: step 2 and 3 are combined here
                broadcastProtocolProgress(stmNextState);
                // Encrypt and send response
                if (rawMsg != null) {
                    byte[] encryptedMsg = crypto.encryptPlaintext(rawMsg);
                    Message responseMsg = Message.obtain(null, MSG_NFC_SEND_PACKET);
                    responseMsg.obj = encryptedMsg;
                    try {
                        mNfcCardEmulationService.send(responseMsg);
                    } catch (RemoteException e) {
                        Log.e("TAG", "Response not sent; RemoteException calling into " +
                                "CardEmulationService.");
                    }
                } else {
                    Log.e(TAG, "Could not send message due to lack of input data.");
                }
            }
            break;

            /**
             * Connection lost to interfacing NFC terminal.
             *
             * If the current protocol was not finished, there is a problem with the terminal
             * or our behavior (e.g., invalid account).
             */
            case MSG_NFC_CONNECTION_LOST: {
                if (stmNextState < PROTO_MSG3_SEND_TOKEN_AND_NONCE) {
                    // The protocol specification could not be fulfilled.
                    // Communication was aborted in-between. Have to notify the user.
                    broadcastProgress(1, stmNextState);
                }
            }
            break;
        }
    }

    private void resetStates() {
        stmNextState = PROTO_MSG1_TUM_ID_AND_NONCE;
        r_T = r_S = "";
    }

    private void initCryptoOnce() {
        if (crypto == null) {
            // Initialize crypto
            crypto = new RSACrypto(appContext);
            try {
                //crypto.loadPrivateKeyFromResource(R.raw.private_key_android);
                //crypto.initDecryption();

                // Note: public key should be loaded dynamically in production
                crypto.loadPublicKeyFromResource(R.raw.public_key_android);
                crypto.initEncryption();
            } catch (InvalidKeyException e) {
                e.printStackTrace();
            }
        }
        // Always reload our private key because it might be a new one
        crypto.loadPrivateKeyFromPrefs();

    }

    /**
     * Protocol step 1: pseudo_student_ID and fresh nonce
     *
     * @return Outgoing message to be sent.
     */
    private byte[] sendStudentIdAndNonce() {
        byte[] output = null;

        // Generate random nonce r_S
        SecureRandom secureRandom = new SecureRandom();
        byte[] rawNonce = new byte[32];
        secureRandom.nextBytes(rawNonce);
        r_S = Base64.encodeToString(rawNonce, Base64.NO_WRAP);
        // Get pseudo student ID from preferences
        SharedPreferences prefs = appContext.getSharedPreferences(
                MainActivity.TUM_GETIN_PREFERENCES,
                Context.MODE_PRIVATE | Context.MODE_MULTI_PROCESS
        );
        String pseudoID = prefs.getString("pseudo_ID", "");
        Log.d(TAG, "Sending pseudo_ID: " + pseudoID);

        // Prepare outgoing message: Android --> target terminal
        JSONObject jsonMsg = new JSONObject();
        try {
            jsonMsg.put("type", PROTO_MSG1_TUM_ID_AND_NONCE);   //< Message type
            jsonMsg.put("pid", pseudoID);                       //< Pseudo ID
            jsonMsg.put("rs", r_S);                             //< Random nonce r_S

            Log.d(TAG, "Sending pseudoID and nonce: " + jsonMsg.toString());
            output = jsonMsg.toString().getBytes(Charset.forName("UTF-8"));
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return output;
    }

    /**
     * Protocol step 3: compare nonce we sent to terminal with received one and forward
     * terminal's nonce albeit some additional data
     *
     * Note: the protocol specifies to send the user token to the terminal. Currently, we omit
     * this step, because this doesn't seem to improve security.
     *
     * @param msgIn Message received from the terminal.
     * @return Outgoing message to be sent.
     */
    private byte[] checkResponseFromTerminal(String msgIn) {
        byte[] output = null;
        boolean matchingNonceRS = false;
        try {
            JSONObject jsonMsg = new JSONObject(msgIn);
            int msgType = jsonMsg.getInt("type");
            String msgR_S = jsonMsg.getString("rs");
            String msgR_T = jsonMsg.getString("rt");
            Log.i(TAG, "type: " + msgType + ", r_S: " + msgR_S + ", r_T: " + msgR_T);

            Log.d(TAG, "Expected r_S: " + r_S);
            if (r_S.equals(msgR_S)) {
                // Terminal could decrypt our message correctly with out fresh nonce
                r_T = msgR_T;
                matchingNonceRS = true;
            } else {
                Log.e(TAG, "Received nonce r_S not the same as the one we sent. Replay attack?");
                // TODO: generate error, abort communication or hide this detection to prevent oracle attacks
                // TODO: reset state machine
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        if (matchingNonceRS) {
            // Everything fine, generate response including our salted token hash
            // and the terminal's nonce r_T

            // Generate hash from salted token
            SharedPreferences prefs = appContext.getSharedPreferences(
                    MainActivity.TUM_GETIN_PREFERENCES,
                    appContext.MODE_PRIVATE
            );
            String salt = prefs.getString("salt", "");
            String token = prefs.getString("tumOnlineToken", "");
            Log.d(TAG, "Salt: " + salt + ", token: " + token);
            String tokenHash = "";
            try {
                // sha256(salt + token) as base64
                byte[] c = (salt + token).getBytes("UTF-8");
                MessageDigest hashFunc = MessageDigest.getInstance("SHA-256");
                hashFunc.reset();
                byte[] hashedToken = hashFunc.digest(c);
                tokenHash = Base64.encodeToString(hashedToken, Base64.NO_WRAP);
                Log.d(TAG, "Salted token hash: " + tokenHash);
            } catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
                e.printStackTrace();
            }

            // Prepare outgoing protocol message
            JSONObject jsonMsg = new JSONObject();
            try {
                jsonMsg.put("type", PROTO_MSG3_SEND_TOKEN_AND_NONCE);   //< Message type
                jsonMsg.put("htoken", tokenHash);                       //< Hashed user token
                jsonMsg.put("rt", r_T);                                 //< Received nonce r_T

                output = jsonMsg.toString().getBytes(Charset.forName("UTF-8"));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        return output;
    }

    private void broadcastProtocolProgress(int finishedStep) {
        // Status code 0: protocol step finished successfully
        broadcastProgress(0, finishedStep);
    }

    private void broadcastProgress(int statusCode, int relatedStep) {
        Log.d(TAG, "Broadcasting protocol status " + statusCode + " in step " + relatedStep);
        Intent intent = new Intent(INTENT_PROTOCOL_PROGRESS);
        intent.putExtra("status", statusCode);
        intent.putExtra("progress", relatedStep);
        LocalBroadcastManager.getInstance(appContext).sendBroadcast(intent);
    }
}