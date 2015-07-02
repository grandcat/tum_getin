package com.tca.mobiledooraccess.nfc;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.nfc.cardemulation.HostApduService;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

import com.tca.mobiledooraccess.service.MessageExchangeService;
import com.tca.mobiledooraccess.service.StatefulProtocolHandler;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
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
    ByteArrayInputStream dataOut;   // is ByteArrayInputStream
    AtomicBoolean dataOutReady = new AtomicBoolean(false);
    ByteArrayOutputStream dataIn = new ByteArrayOutputStream();
    AtomicBoolean dataInReady = new AtomicBoolean(true);

    /**
     * NFC service messenger
     *
     * - mNfcMessagingService:
     *   Incoming packets (dataIn) are sent to the NFC state machine for the application logic.
     * - mResponseMessenger:
     *   Response channel for the NFC state machine to generate packets to be transmitted to the
     *   client (dataOut).
     */
    private Messenger mNfcMessagingService;
    final Messenger mResponseMessenger = new Messenger(new MsgHandler());

    /**
     * Outgoing messages
     */
    final class MsgHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case StatefulProtocolHandler.MSG_NFC_SEND_PACKET:
                    /**
                     * Prepare outgoing message to be sent to the NFC terminal.
                     */
                    byte[] data = (byte[])msg.obj;
                    if (!dataOutReady.get()) {
                        // DataOut was sent already.
                        // We can safely replace the buffer now
                        if (data != null) {
                            dataOut = new ByteArrayInputStream(data);
                            dataOut.reset();
                            dataOutReady.set(true);
                            Log.d(TAG, "dataOut message with " + dataOut.available() + " bytes " +
                                    "queued for transmission to client.");
                        } else {
                            Log.e(TAG, "NULL message received on ResponseMessenger handler.");
                        }

                    } else {
                        // DataOut buffer still occupied
                        Log.e(TAG, "DataOut buffer still occupied. Data will be lost.");
                    }
                    break;
            }
        }
    }

    /**
     * Class for interacting with the main interface of the service.
     */
    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mNfcMessagingService = new Messenger(service);
            Log.i(TAG, "onServiceConnected in CardEmulation with mNfcMessagingService: " + mNfcMessagingService.toString());
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            // FIX: won't remove it currently as it might be useful for a second try to connect
            // to an NFC terminal
            // mNfcMessagingService = null;
            Log.i(TAG, "onServiceDisconnected in CardEmulation");
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "CardEmulation onCreate called.");
        // Bind to NFC Messaging service
        Intent nfcService = new Intent(getApplicationContext(), MessageExchangeService.class);
        getApplicationContext().bindService(nfcService, mConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mNfcMessagingService != null) {
            getApplicationContext().unbindService(mConnection);
        }
        Log.i(TAG, "CardEmulation service destroyed (state machine can still be online).");
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
        if (connectionEstablished) {
            Message msg = Message.obtain(null, StatefulProtocolHandler.MSG_NFC_CONNECTION_LOST);
            msg.arg1 = reason;
            try {
                mNfcMessagingService.send(msg);
            } catch (RemoteException e) {
                Log.e(TAG, "Could not send status to NFC message exchange service.");
            }
        }
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
             *
             * Only if the correct AID is sent, we will process any further requests.
             * We also check whether binding to the NFC Message Exchange service is ready.
             * If not, the terminal will give us some time until the binding succeeds.
             */
            Log.i(TAG, "NFC terminal is authentic.");

            if (mNfcMessagingService != null) {
                // Successfully connected to the NFC Message exchange service.
                // Activate the communication channel.
                connectionEstablished = true;
                // Inform NFC state machine about new terminal
                Message msg = Message.obtain(null, StatefulProtocolHandler.MSG_NFC_NEW_TERMINAL);
                msg.replyTo = mResponseMessenger;
                try {
                    mNfcMessagingService.send(msg);
                    return APDU.StatusMessage.SUCCESS;
                } catch (RemoteException e) {
                    Log.e(TAG, "Could not send data input to NFC state machine.");
                    e.printStackTrace(); // TODO: remove stacktrace
                }
                Log.d(TAG, "NFC communication channel established.");

            } else {
                // Binding to the NFC message exchange service not ready yet
                // Inform terminal about this to provide some free time.
                Log.e(TAG, "NFC MessageExchange service not bounded. Trying again.");
                return APDU.StatusMessage.ERR_NOT_READY;
            }

            // Something went wrong
            return APDU.StatusMessage.ERR_PARAMS;

        } else if (connectionEstablished) {
            byte[] resultMsg;
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
                    resultMsg = APDU.StatusMessage.ERR_PARAMS;
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
        // If incoming data message complete
        // - transfer data to NFC state machine to do the high-level logic
        // - Clean up internal buffer after that
        if (0 == nextBytes) {
            dataInReady.set(false);
            Log.i(TAG, "Received data with " + dataIn.size() + " bytes complete.");
            // Push to NFC service thread
            Message msg = Message.obtain(null, StatefulProtocolHandler.MSG_NFC_RECEIVED_PACKET);
            msg.obj = dataIn.toByteArray();
            msg.replyTo = mResponseMessenger;
            try {
                mNfcMessagingService.send(msg);
            } catch (RemoteException e) {
                Log.e(TAG, "Could not send data input to NFC statemachine.");
                e.printStackTrace(); // TODO: remove stacktrace
            }
            // Cleanup for new input data
            dataIn.reset();
            dataInReady.set(true);
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

