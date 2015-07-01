package com.tca.mobiledooraccess.service;

import android.content.Context;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

import com.tca.mobiledooraccess.R;
import com.tca.mobiledooraccess.utils.RSACrypto;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.security.InvalidKeyException;

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
    private int stmNextState = 0;

    public StmProtocolHandler() {
        super();
    }

    @Override
    public void handleMessage(Message msg) {
        // process incoming messages here
        Log.d(MessageExchangeService.TAG, "State machine handler is " + Thread.currentThread().getName());
        Log.d(MessageExchangeService.TAG, "Got message with what " + msg.what);

        switch(msg.what) {
            case MSG_NFC_NEW_TERMINAL: {
                /**
                 * Initial message on first contact
                 */
                initCryptoOnce();

                // Always take new messenger because CardEmulation's manager is recreated each time
                // it binds to a new terminal
                mNfcCardEmulationService = msg.replyTo;

                // Protocol step 1
                byte[] raw_msg = sendTokenAndNonce();
                // Encrypt message with terminal's public key
                byte[] encrypted_msg = crypto.encryptPlaintext(raw_msg);
                // Submit encrypted message to NFC card emulation service
                Message responseMsg = Message.obtain(null, MSG_NFC_SEND_PACKET);
                responseMsg.obj = encrypted_msg;
                try {
                    mNfcCardEmulationService.send(responseMsg);
                } catch (RemoteException e) {
                    Log.e("TAG", "Response not sent; RemoteException calling into " +
                            "CardEmulationService.");
                }
            }
            break;

            case MSG_NFC_RECEIVED_PACKET: {
                /**
                 * Processing incoming packet sent by NFC terminal.
                 */
                if (mNfcCardEmulationService == null) mNfcCardEmulationService = msg.replyTo;

                byte[] dataIn = (byte[])msg.obj;
                Log.i(TAG, "Complete packet received [" + dataIn.length + " bytes]\n");
                String s = "";
                try {
                    // Decrypt message
                    byte[] rawMsgIn = crypto.decryptCiphertext(dataIn);
                    s = new String(rawMsgIn, "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                Log.i(TAG, "Decrypted msg: " + s);

                // Test: send reply that should be sent to the client
                byte[] data = ("Last: Hallo, hier spricht das Smartphone. Und es spricht sogar noch mehr!!! " +
                        "Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod " +
                        "tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. At " +
                        "vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren," +
                        " no sea takimata sanctus est Lorem ipsum dolor sit amet. Heheheehhehehehehehehehe" +
                        "ehu und es geht weiter!!!||||--. <-- Sonderzeichen.")
                        .getBytes(Charset.forName("UTF-8"));
                Message responseMsg = Message.obtain(null, MSG_NFC_SEND_PACKET);
                responseMsg.obj = data;
                try {
                    mNfcCardEmulationService.send(responseMsg);
                } catch (RemoteException e) {
                    Log.e("TAG", "Response not sent; RemoteException calling into " +
                            "CardEmulationService.");
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

        JSONObject jsonMsg = new JSONObject();
        try {
            jsonMsg.put("type", PROTO_MSG1_TUM_ID_AND_NONCE);
            jsonMsg.put("tok", "mysecrettokenfromtumonline");
            jsonMsg.put("rs", "randombytes______________32bytes");

            output = jsonMsg.toString().getBytes(Charset.forName("UTF-8"));
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return output;
    }
}