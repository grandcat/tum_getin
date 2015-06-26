package com.tca.mobiledooraccess.nfc;

import android.nfc.cardemulation.HostApduService;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

import com.tca.mobiledooraccess.MessageExchangeThread;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by Stefan on 23.06.2015.
 */
public class CardEmulationService extends HostApduService {
    // AID for our GetInTUM Door Entrance System
    private static final String GETINTUM_AID = "F074756D676574696E02";
    private static final String TAG = "CardEmulationService";
    // ISO-DEP command HEADER for selecting an AID.
    // Format: [Class | Instruction | Parameter 1 | Parameter 2]
    private static final String SELECT_APDU_HEADER = "00A40400";
    private static final byte[] SELECT_APDU = BuildSelectApdu(GETINTUM_AID);
    /**
     * State machine for processing APDUs
     */
    // Config
    public final static int MAX_CHUNK_SIZE = 125;   // size in bytes
    public final static int MAX_BYTES = 4096;       // size in bytes
    // States and progress
    private boolean connectionEstablished = false;
    private int txCount = 0, rxCount = 0;
    // Data buffers
    ByteArrayInputStream dataOut;
    AtomicBoolean dataOutReady = new AtomicBoolean(true);   // TODO: change to false after test
    ByteArrayOutputStream dataIn = new ByteArrayOutputStream();
    AtomicBoolean dataInReady = new AtomicBoolean(true);
    // Communication
    private MessageExchangeThread msgExThread;

    public CardEmulationService() {
        super();
        // Create separate thread for doing the protocol logic
        Log.d(TAG, "NFCCardEmulation is: " + Thread.currentThread().getName());
//        msgExThread = new MessageExchangeThread();
//        msgExThread.start();
        // Simple test with direct handler
//        Handler mHandler = msgExThread.getHandler();
//        Message msg = mHandler.obtainMessage(2);
//        msg.arg1 = 123;
//        mHandler.sendMessage(msg);
//        // Suggested way
//        Messenger mMessenger = new Messenger(msgExThread.getHandler());
//        Message msg2 = Message.obtain(msgExThread.getHandler(), 1);
//        msg2.arg1 = 456;
//        try {
//            mMessenger.send(msg2);
//        } catch (RemoteException e) {e.printStackTrace();}

        // TEST: add some test data for transmission to terminal
        byte[] data = ("Hallo, hier spricht das Smartphone. Und es spricht sogar noch mehr!!! " +
                "Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod " +
                "tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. At " +
                "vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren," +
                " no sea takimata sanctus est Lorem ipsum dolor sit amet. Heheheehhehehehehehehehe" +
                "ehu und es geht weiter!!!||||--. <-- Sonderzeichen.")
                .getBytes(Charset.forName("UTF-8"));
        dataOut = new ByteArrayInputStream(data);
    }

    /**
     * Called if the connection to the NFC card is lost, in order to let the application know the
     * cause for the disconnection (either a lost link, or another AID being selected by the
     * reader).
     *
     * @param reason Either DEACTIVATION_LINK_LOSS or DEACTIVATION_DESELECTED
     */
    @Override
    public void onDeactivated(int reason) {
        // msgExThread.getLooper().quit();
        Log.i(TAG, "Lost connection to NFC reader.");
    }

    /**
     * This method will be called when a command APDU has been received from a remote device. A
     * response APDU can be provided directly by returning a byte-array in this method. In general
     * response APDUs must be sent as quickly as possible, given the fact that the user is likely
     * holding his device over an NFC reader when this method is called.
     *
     * <p class="note">If there are multiple services that have registered for the same AIDs in
     * their meta-data entry, you will only get called if the user has explicitly selected your
     * service, either as a default or just for the next tap.
     *
     * <p class="note">This method is running on the main thread of your application. If you
     * cannot return a response APDU immediately, return null and use the {@link
     * #sendResponseApdu(byte[])} method later.
     *
     * @param commandApdu The APDU that received from the remote device
     * @param extras A bundle containing extra data. May be null.
     * @return a byte-array containing the response APDU, or null if no response APDU can be sent
     * at this point.
     */
    @Override
    public byte[] processCommandApdu(byte[] commandApdu, Bundle extras) {
        Log.i(TAG, "Received APDU: " + APDU.ByteArrayToHexString(commandApdu));
        // If the APDU matches the SELECT AID command for this service,
        // send the loyalty card account number, followed by a SELECT_OK status trailer (0x9000).

        if (Arrays.equals(SELECT_APDU, commandApdu)) {
            /**
             * Authentication of NFC terminal
             * Only if the correct AID is sent, we will process any further requests.
             */
            connectionEstablished = true;
            Log.i(TAG, "NFC terminal authenticated successfully.");
            byte[] accountBytes = new byte[] {(byte)0x11, (byte)0x22, (byte)0x33};
            return concatByteArrays(accountBytes, APDU.StatusMessage.SUCCESS);

        } else if (connectionEstablished) {
            byte[] resultMsg = APDU.StatusMessage.ERR_PARAMS;
            // Simple state machine
            switch (commandApdu[APDU.Header.INS]) {
                case APDU.Instruction.ISO7816_READ_DATA:
                    txCount++;
                    Log.d(TAG, "Sent fragment " + txCount);
                    resultMsg = pushDataChunk(commandApdu);
                    break;

                case APDU.Instruction.ISO7816_WRITE_DATA:
                    rxCount++;
                    Log.d(TAG, "Receiving fragment " + rxCount);
                    resultMsg = receiveDataChunk(commandApdu);
                    break;

                default:
                    return APDU.StatusMessage.ERR_PARAMS;
            }

            return resultMsg;

        } else {
            // No valid request due to missing login AID
            return APDU.StatusMessage.ERR_PARAMS;
        }
    }

    /**
     * Push data fragments to the terminal if available
     * @return Status response
     */
    private byte[] pushDataChunk(final byte[] commandApdu) {
        // Only start transmission if tx buffer is ready
        if (!dataOutReady.get()) {
            return APDU.StatusMessage.ERR_NO_DATA;
        }

        int requestedSize = APDU.getHeaderValue(commandApdu, APDU.Header.LC);
        requestedSize = Math.min(requestedSize, MAX_CHUNK_SIZE);
        int msgLength = Math.min(requestedSize, dataOut.available());
        // Construct payload + status code
        byte[] rawOutMsg = new byte[msgLength + 2];

        int writtenBytes = dataOut.read(rawOutMsg, 0, msgLength);
        int nextBytes = Math.min(dataOut.available(), MAX_CHUNK_SIZE);
        Log.d(TAG, "Sending chunk of " + writtenBytes + " bytes, next will be " + nextBytes + " bytes.");

        if (nextBytes > 0) {
            // Announce available bytes for the next transmission
            byte[] status = new byte[]{(byte) 0x61, (byte)(nextBytes & 0xff)};
            APDU.insertStatusMessage(rawOutMsg, status);
        } else {
            // Stop communication
            APDU.insertStatusMessage(rawOutMsg, APDU.StatusMessage.SUCCESS);
            dataOutReady.set(false);
        }

        return rawOutMsg;
    }

    /**
     * Receive fragment from the terminal and assemble the bytes in an internal buffer
     * @return Status response
     */
    private byte[] receiveDataChunk(final byte[] commandApdu) {
        if (!dataInReady.get()) {
            // Previously received data was not fetched yet
            return APDU.StatusMessage.ERR_NOT_READY;
        }

        int dataLen = commandApdu.length - APDU.Header.LEN;
        if (dataLen > MAX_CHUNK_SIZE)
            // Invalid size of input message
            return APDU.StatusMessage.ERR_PARAMS;
        else if ((dataIn.size() + dataLen) > MAX_BYTES) {
            // Received data is not complete; give client a new try
            dataIn.reset();
            return APDU.StatusMessage.ERR_PARAMS;
        }

        // Assembling fragmented data messages in local buffer
        dataIn.write(commandApdu, APDU.Header.DATA, dataLen);
        // Check if all data chunks are complete
        int nextBytes = APDU.getHeaderValue(commandApdu, APDU.Header.LC);
        if (0 == nextBytes) {
            // Finish up
            dataInReady.set(false);
            Log.i(TAG, "Received data with " + dataIn.size() + " bytes complete.");
            try {
                String s = new String(dataIn.toByteArray(), "UTF-8");
                Log.i(TAG, s);
            } catch (Exception e) {
                Log.i(TAG, "Could not convert input data to UTF-8");
            }
        }

        return APDU.StatusMessage.SUCCESS;
    }

    /**
     * Build APDU for SELECT AID command. This command indicates which service a reader is
     * interested in communicating with. See ISO 7816-4.
     *
     * @param aid Application ID (AID) to select
     * @return APDU for SELECT AID command
     */
    public static byte[] BuildSelectApdu(String aid) {
        // Format: [CLASS | INSTRUCTION | PARAMETER 1 | PARAMETER 2 | LENGTH | DATA]
        return APDU.HexStringToByteArray(SELECT_APDU_HEADER + String.format("%02X",
                aid.length() / 2) + aid);
    }

    /**
     * Utility method to concatenate two byte arrays.
     * @param first First array
     * @param rest Any remaining arrays
     * @return Concatenated copy of input arrays
     */
    public static byte[] concatByteArrays(byte[] first, byte[]... rest) {
        int totalLength = first.length;
        for (byte[] array : rest) {
            totalLength += array.length;
        }
        byte[] result = Arrays.copyOf(first, totalLength);
        int offset = first.length;
        for (byte[] array : rest) {
            System.arraycopy(array, 0, result, offset, array.length);
            offset += array.length;
        }
        return result;
    }
}

