package com.tca.mobiledooraccess.service;

import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;

/**
 * State machine for NFC high-level logic and protocol
 */
public final class StmProtocolHandler extends BaseMsgHandler {
    private static final String TAG = "StmProtocolHandler";

    public final static int MSG_NFC_CONNECTION_LOST = 0;
    public final static int MSG_NFC_NEW_TERMINAL = 1;
    public final static int MSG_NFC_RECEIVED_PACKET = 2;
    public final static int MSG_NFC_SEND_PACKET = 3;

    private Messenger mNfcEmulationService;

    public StmProtocolHandler() { super(); }

    @Override
    public void handleMessage(Message msg) {
        // process incoming messages here
        Log.d(MessageExchangeService.TAG, "B LooperThread is: " + Thread.currentThread().getName());
        Log.d(MessageExchangeService.TAG, "Got message with arg1 " + msg.arg1);

        switch(msg.what) {
            case MSG_NFC_NEW_TERMINAL: {
                /**
                 * Initial message on first contact
                 */
                if (mNfcEmulationService == null) mNfcEmulationService = msg.replyTo;
                // Test: send reply that should be sent to the client
                byte[] data = ("First: Hallo, hier spricht das Smartphone. Und es spricht sogar noch mehr!!! " +
                        "Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod " +
                        "tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. At " +
                        "vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren," +
                        " no sea takimata sanctus est Lorem ipsum dolor sit amet. Heheheehhehehehehehehehe" +
                        "ehu und es geht weiter!!!||||--. <-- Sonderzeichen.")
                        .getBytes(Charset.forName("UTF-8"));
                Message responseMsg = Message.obtain(null, MSG_NFC_SEND_PACKET);
                responseMsg.obj = data;
                try {
                    mNfcEmulationService.send(responseMsg);
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
                if (mNfcEmulationService == null) mNfcEmulationService = msg.replyTo;

                byte[] dataIn = (byte[]) msg.obj;
                String s = "";
                try {
                    s = new String(dataIn, "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                Log.i(TAG, "Complete packet received [" + dataIn.length + " bytes] :\n" +
                        s);

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
                    mNfcEmulationService.send(responseMsg);
                } catch (RemoteException e) {
                    Log.e("TAG", "Response not sent; RemoteException calling into " +
                            "CardEmulationService.");
                }
            }
            break;
        }
    }
}