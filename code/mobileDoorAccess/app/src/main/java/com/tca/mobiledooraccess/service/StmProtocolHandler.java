package com.tca.mobiledooraccess.service;

import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Base64;
import android.util.Log;

import com.tca.mobiledooraccess.R;
import com.tca.mobiledooraccess.utils.RSACrypto;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.security.InvalidKeyException;
import java.security.SecureRandom;

/**
 * State machine for NFC high-level logic and protocol
 */
public final class StmProtocolHandler extends BaseMsgHandler {
    private static final String TAG = "StmProtocolHandler";
    // NFC protocol
    public final static int MSG_NFC_CONNECTION_LOST = 0;
    public final static int MSG_NFC_NEW_TERMINAL = 1;
    public final static int MSG_NFC_RECEIVED_PACKET = 2;
    public final static int MSG_NFC_SEND_PACKET = 3;

    private Messenger mNfcCardEmulationService;

    // Public / Private Key Cryptography
    private RSACrypto crypto;

    /**
     * Protocol state machine
     * Includes type of all possible states and the next state.
     */
    public final static int PROTO_MSG1_TUM_ID_AND_NONCE = 1;
    public final static int PROTO_MSG2_RECEIVE_NONCE = 2;
    public final static int PROTO_MSG3_SEND_TOKEN_AND_NONCE = 3;
    private int stmNextState = 1;

    private String r_S = "";    //< Random nonce r_S
    private String r_T = "";    //< Received nonce r_T from terminal

    public StmProtocolHandler() {
        super();
    }

    @Override
    public void handleMessage(Message msg) {
        // process incoming messages here
        Log.d(MessageExchangeService.TAG, "State machine handler is " + Thread.currentThread().getName());
        Log.d(MessageExchangeService.TAG, "Got message with what " + msg.what);

        switch (msg.what) {
            case MSG_NFC_NEW_TERMINAL: {
                /**
                 * Initial message on first contact
                 */
                r_T = r_S = "";
                initCryptoOnce();

                // Always take new messenger because CardEmulation's manager is recreated each time
                // it binds to a new terminal
                mNfcCardEmulationService = msg.replyTo;

                // Protocol step 1
                byte[] rawMsg = sendTokenAndNonce();
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

                // Go to next step in state machine: receiving a message from the terminal
                stmNextState = PROTO_MSG2_RECEIVE_NONCE;
            }
            break;

            case MSG_NFC_RECEIVED_PACKET: {
                /**
                 * Processing incoming packet sent by NFC terminal.
                 */
                if (mNfcCardEmulationService == null) mNfcCardEmulationService = msg.replyTo;

                byte[] dataIn = (byte[])msg.obj;
                Log.i(TAG, "Complete packet received [" + dataIn.length + " bytes]\n");
                String msgIn = "";
                try {
                    // Decrypt message
                    byte[] rawMsgIn = crypto.decryptCiphertext(dataIn);
                    msgIn = new String(rawMsgIn, "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                Log.i(TAG, "Decrypted msg: " + msgIn);

                byte[] rawMsg = null;
                switch (stmNextState) {
                    case PROTO_MSG2_RECEIVE_NONCE: {
                        Log.d(TAG, "Current stm step: PROTO_MSG2_RECEIVE_NONCE");
                        // TODO: add try block
                        rawMsg = checkResponseFromTerminal(msgIn);

                        stmNextState = PROTO_MSG3_SEND_TOKEN_AND_NONCE;
                    }
                    break;
                    // Todo: default block to reset state machine in case of error
                }
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
        }
    }

    private void initCryptoOnce() {
        if (crypto == null) {
            // Initialize crypto
            crypto = new RSACrypto(appContext);
            try {
                crypto.loadPrivateKeyFromResource(R.raw.private_key_android);
                crypto.initDecryption();
                // Note: public key should be loaded dynamically in production
                crypto.loadPublicKeyFromResource(R.raw.public_key_android);
                crypto.initEncryption();
            } catch (InvalidKeyException e) {
                e.printStackTrace();
            }
        }

    }

    private byte[] sendTokenAndNonce() {
        byte[] output = null;

        // Generate random nonce r_S
        SecureRandom secureRandom = new SecureRandom();
        byte[] rawNonce = new byte[32];
        secureRandom.nextBytes(rawNonce);
        r_S = Base64.encodeToString(rawNonce, Base64.DEFAULT);

        JSONObject jsonMsg = new JSONObject();
        try {
            jsonMsg.put("type", PROTO_MSG1_TUM_ID_AND_NONCE);   //< Message type
            jsonMsg.put("pid", "Todo: pseudoID_inserthere");    //< Pseudo ID
            jsonMsg.put("rs", r_S);                             //< Random nonce r_S

            output = jsonMsg.toString().getBytes(Charset.forName("UTF-8"));
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return output;
    }

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
            // Everything fine, generate response including our token and the terminal's nonce r_T
            JSONObject jsonMsg = new JSONObject();
            try {
                jsonMsg.put("type", PROTO_MSG3_SEND_TOKEN_AND_NONCE);   //< Message type
                jsonMsg.put("tok", "Todo: tokeninserthere");            //< Pseudo ID
                jsonMsg.put("rt", r_T);                                 //< Random nonce r_S

                output = jsonMsg.toString().getBytes(Charset.forName("UTF-8"));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        return output;
    }
}