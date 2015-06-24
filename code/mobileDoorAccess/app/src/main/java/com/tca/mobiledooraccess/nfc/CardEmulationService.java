package com.tca.mobiledooraccess.nfc;

import android.nfc.cardemulation.HostApduService;
import android.os.Bundle;
import android.util.Log;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;

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
    // States and progress
    private boolean connectionEstablished = false;
    private int txCount = 0;
    // Data buffers
    ByteArrayInputStream dataOut;
    ByteArrayOutputStream dataIn;

    public CardEmulationService() {
        super();
        // TEST: add some test data for transmission to terminal
        byte[] data = ("Hallo, hier spricht das Smartphone. Und es spricht sogar noch mehr!!! " +
                "Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod " +
                "tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. At " +
                "vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren," +
                " no sea takimata sanctus est Lorem ipsum dolor sit amet. Heheheehhehehehehehehehe" +
                "ehu und es geht weiter!!!||||~~~++#+²²²²üöä. <-- Sonderzeichen.")
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
            Log.i(TAG, "Sending account number: " + accountBytes.toString());
            return concatByteArrays(accountBytes, APDU.StatusMessage.SUCCESS);

        } else if (connectionEstablished) {
            byte[] resultMsg = APDU.StatusMessage.ERR_PARAMS;
            // Simple state machine
            switch (commandApdu[APDU.Header.INS]) {
                case APDU.Instruction.ISO7816_READ_DATA:
                    txCount++;
                    Log.d(TAG, "Sent fragments: " + txCount);
                    resultMsg = pushDataChunk(commandApdu);

                    break;

                case APDU.Instruction.ISO7816_WRITE_DATA:

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
     * @return message consisting of one fragment.
     */
    private byte[] pushDataChunk(final byte[] commandApdu) {
        int requestedSize = APDU.getHeaderValue(commandApdu, APDU.Header.LC);
        requestedSize = Math.min(requestedSize, MAX_CHUNK_SIZE);
        // Construct payload + status code
        byte[] rawOutMsg = new byte[requestedSize + 2];

        int writtenBytes = dataOut.read(rawOutMsg, 0, requestedSize);
        int nextBytes = Math.min(dataOut.available(), MAX_CHUNK_SIZE);
        Log.d(TAG, "Sending chunk of " + writtenBytes + " bytes, next will be " + nextBytes + " bytes.");

        if (nextBytes > 0) {
            // Announce available bytes for the next transmission
            byte[] status = new byte[]{(byte) 0x61, (byte)(nextBytes & 0xff)};
            APDU.insertStatusMessage(rawOutMsg, status);
        } else {
            // Stop communication
            APDU.insertStatusMessage(rawOutMsg, APDU.StatusMessage.SUCCESS);
        }

        return rawOutMsg;
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

