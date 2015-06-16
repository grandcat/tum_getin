/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.android.cardreader;

import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.IsoDep;

import com.example.android.common.logger.Log;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Arrays;

/**
 * Callback class, invoked when an NFC card is scanned while the device is running in reader mode.
 *
 * Reader mode can be invoked by calling NfcAdapter
 */
public class LoyaltyCardReader implements NfcAdapter.ReaderCallback {
    private static final String TAG = "LoyaltyCardReader";
    // AID for our loyalty card service.
    // private static final String SAMPLE_LOYALTY_CARD_AID = "d2760000850101";
    private static final String SAMPLE_LOYALTY_CARD_AID = "74756d676574696e01";
    // ISO-DEP command HEADER for selecting an AID.
    // Format: [Class | Instruction | Parameter 1 | Parameter 2]
    private static final String SELECT_APDU_HEADER = "00A40400";
    private static final String APDU_READ_BINARY = "00B0";
    private static final String APDU_READ_BINARY_SIZE = "00CBB0";
    private static final byte[] APDU_WRITE_BINARY = new byte[] {(byte)0x00, (byte)0xd0, (byte)0x00, (byte)0x00, (byte)0x00};
    private static final byte[] APDU_GET_DATA_SIZE = new byte[] {(byte)0x00, (byte)0xcb, (byte)0xb0, (byte)0x00, (byte)0x00};
    private static final byte[] APDU_SET_DATA_SIZE = new byte[] {(byte)0x00, (byte)0xcb, (byte)0xd0};
    // "OK" status word sent in response to SELECT AID command (0x9000)
    private static final byte[] SELECT_OK_SW = {(byte) 0x90, (byte) 0x00};

    private static final int CHUNK_SIZE = 125;

    // Weak reference to prevent retain loop. mAccountCallback is responsible for exiting
    // foreground mode before it becomes invalid (e.g. during onPause() or onStop()).
    private WeakReference<AccountCallback> mAccountCallback;

    public interface AccountCallback {
        public void onAccountReceived(String account);
    }

    public LoyaltyCardReader(AccountCallback accountCallback) {
        mAccountCallback = new WeakReference<AccountCallback>(accountCallback);
    }

    /**
     * Callback when a new tag is discovered by the system.
     *
     * <p>Communication with the card should take place here.
     *
     * @param tag Discovered tag
     */
    @Override
    public void onTagDiscovered(Tag tag) {
        Log.i(TAG, "New tag discovered");
        // Android's Host-based Card Emulation (HCE) feature implements the ISO-DEP (ISO 14443-4)
        // protocol.
        //
        // In order to communicate with a device using HCE, the discovered tag should be processed
        // using the IsoDep class.
        IsoDep isoDep = IsoDep.get(tag);
        // NfcA nfcA = NfcA.get(tag);
        if (isoDep != null) {
            try {
                // Connect to the remote NFC device
                isoDep.connect();
                isoDep.setTimeout(1200);
                // Details about connection
                Log.i(TAG, "MaxTransceiveLength: " + isoDep.getMaxTransceiveLength());
                // Build SELECT AID command for our loyalty card service.
                // This command tells the remote device which service we wish to communicate with.
                Log.i(TAG, "Requesting remote AID: " + SAMPLE_LOYALTY_CARD_AID);
                byte[] old_command = BuildSelectApdu(SAMPLE_LOYALTY_CARD_AID, SELECT_APDU_HEADER);
                // Send command to remote device
                Log.i(TAG, "Sending: " + ByteArrayToHexString(old_command));
                byte[] result = isoDep.transceive(old_command);
                // If AID is successfully selected, 0x9000 is returned as the status word (last 2
                // bytes of the result) by convention. Everything before the status word is
                // optional payload, which is used here to hold the account number.
                int resultLength = result.length;
                byte[] statusWord = {result[resultLength-2], result[resultLength-1]};
                byte[] payload = Arrays.copyOf(result, resultLength-2);
                Log.i(TAG, "Raw Received: " + ByteArrayToHexString(result));
                if (Arrays.equals(SELECT_OK_SW, statusWord)) {
                    ByteBuffer command = ByteBuffer.allocate(5);
                    // Try to transmit some chunked data to reader
                    byte[] outText = ("Hallo, hier spricht das Smartphone. Hier ein wenig UTF.8 Text. " +
                            "Dieser muss jedoch bald durch eine verschluesselte Nachricht ausgetauscht" +
                            " werden.\nLorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam " +
                            "nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, " +
                            "sed diam voluptua").getBytes(Charset.forName("UTF-8"));
                    short data_out_size = (short)outText.length;

                    command.clear();
                    command.put(APDU_SET_DATA_SIZE);
                    command.putShort(3, (short) (data_out_size & 0xffff));
                    Log.i(TAG, "Request " + data_out_size + " for writing. Raw: " + ByteArrayToHexString(command.array()));
                    byte[] response = isoDep.transceive(command.array());
                    Log.i(TAG, "Raw Received: " + ByteArrayToHexString(response));
                    // Transfer data to Reader
                    int offset = 0;
                    while (offset < data_out_size) {
                        int remaining_bytes = data_out_size - offset;
                        int out_len = Math.min(CHUNK_SIZE, remaining_bytes);

                        byte[] raw_out = new byte[5 + out_len];
                        ByteBuffer buf_out = ByteBuffer.wrap(raw_out);
                        buf_out.put(APDU_WRITE_BINARY);
                        buf_out.put(outText, offset, out_len);
                        Log.i(TAG, "remaining: " + remaining_bytes + ", offset: " + offset + ", out_len: " + out_len);

                        Log.i(TAG, "Send data Raw: " + ByteArrayToHexString(raw_out));
                        byte[] response2 = isoDep.transceive(raw_out);
                        Log.i(TAG, "Raw Received: " + ByteArrayToHexString(response2));

                        offset += CHUNK_SIZE;
                    }

                    // Try to send some more data, but actually we know that the buffer is full
//                    command.clear();
//                    command.put(APDU_SET_DATA_SIZE);
//                    command.putShort(3, (short) (data_out_size & 0xffff));
//                    Log.i(TAG, "Fake: Request " + data_out_size + " for writing. Raw: " + ByteArrayToHexString(command.array()));
//                    response = isoDep.transceive(command.array());
//                    Log.i(TAG, "Raw Received: " + ByteArrayToHexString(response));

                    // Ask for available data
                    int data_size = waitForReadyInput(isoDep);
                    // Retrieve binary data chunck by chunck
                    offset = 0;
                    ByteBuffer data_in = ByteBuffer.allocate(Math.min(data_size, 4096));
                    command = ByteBuffer.allocate(5);
                    command.put(1, (byte) 0xB0);
                    // ByteStream a:
                    while (offset < data_size)
                    {
                        int req_size = Math.min((data_size - offset), CHUNK_SIZE);

                        // command.putShort(2, (short) (offset & 0xffff)); // interpret as unsigned
                        command.put(4, (byte) (req_size & 0xff));       // LE: requested size of response

                        Log.i(TAG, "Fetch data at offset " + offset + " Raw: " + ByteArrayToHexString(command.array()));
                        byte[] res = isoDep.transceive(command.array());

                        Log.i(TAG, "Raw Received: " + ByteArrayToHexString(res));
                        data_in.put(res, 0, Math.min(req_size, res.length - 2));
                        offset += res.length - 2;
                    }
                    // Finished joining junks
                    Log.i(TAG, "Buffered output: " + ByteArrayToHexString(data_in.array()));
                    Log.i(TAG, "Offset: " + offset);
                    String s = new String(data_in.array(), "UTF-8");
                    Log.i(TAG, "Got text:\n" + s);

                    /*resultLength = result.length;
                    payload = Arrays.copyOf(result, resultLength-2);
                    Log.i(TAG, "Payload: " + ByteArrayToHexString(payload));

                    // The remote NFC device will immediately respond with its stored account number
                    String accountNumber = new String(payload, "UTF-8");
                    Log.i(TAG, "Received: " + accountNumber);
                    // Inform CardReaderFragment of received account number
                    mAccountCallback.get().onAccountReceived(accountNumber);*/
                }
            } catch (IOException e) {
                Log.e(TAG, "Error communicating with card: " + e.toString());
            }
        }
    }

    public static int waitForReadyInput(IsoDep isoDep) throws IOException {
        Log.i(TAG, "Waiting for data from NFC reader...");
        for (int i = 0; i < 100; ++i) {
            byte[] response = isoDep.transceive(APDU_GET_DATA_SIZE);
            // Ignore error message
            if (2 == response.length)
            {
                continue;
            } else {
                // Got a positive answer (4 bytes)
                ByteBuffer result = ByteBuffer.wrap(response);
                int data_size = result.getShort() & 0xffff;
                Log.i(TAG, "Data available of size " + data_size + "bytes.");
                return data_size;
            }
        }

        return -1;
    }

    /**
     * Build APDU for SELECT AID command. This command indicates which service a reader is
     * interested in communicating with. See ISO 7816-4.
     *
     * @param aid Application ID (AID) to select
     * @return APDU for SELECT AID command
     */
    public static byte[] BuildSelectApdu(String aid, String header) {
        // Format: [CLASS | INSTRUCTION | PARAMETER 1 | PARAMETER 2 | LENGTH | DATA]
        return HexStringToByteArray(header + String.format("%02X", aid.length() / 2) + aid);
    }

    /**
     * Utility class to convert a byte array to a hexadecimal string.
     *
     * @param bytes Bytes to convert
     * @return String, containing hexadecimal representation.
     */
    public static String ByteArrayToHexString(byte[] bytes) {
        final char[] hexArray = {'0','1','2','3','4','5','6','7','8','9','A','B','C','D','E','F'};
        char[] hexChars = new char[bytes.length * 2];
        int v;
        for ( int j = 0; j < bytes.length; j++ ) {
            v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    /**
     * Utility class to convert a hexadecimal string to a byte string.
     *
     * <p>Behavior with input strings containing non-hexadecimal characters is undefined.
     *
     * @param s String containing hexadecimal characters to convert
     * @return Byte array generated from input
     */
    public static byte[] HexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }

}
