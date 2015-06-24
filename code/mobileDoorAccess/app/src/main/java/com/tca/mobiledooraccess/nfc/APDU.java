package com.tca.mobiledooraccess.nfc;

/**
 * Created by Stefan on 24.06.2015.
 */
public final class APDU {

    public static final int ISO7816_SHORT_APDU_MAX_LEN = 256 + 4 + 2;

    /**
     * C-APDU offsets for header and payload
     * [CLA | INS | P1 | P2 | LC | DATA]
     */
    public static final class Header {
        // Offsets of header fields
        public static final int CLA     = 0;    // Class
        public static final int INS     = 1;    // Instruction: action defined by ISO7816
        public static final int P1      = 2;    // Parameter 1
        public static final int P2      = 3;    // Parameter 2
        public static final int LC      = 4;    // Length command LC or LE
        public static final int DATA    = 5;    // Payload
        // Header length
        public static final int LEN     = DATA;
    }

    /**
     * ISO7816-4 instructions
     */
    public static final class Instruction {
        public static final byte ISO7816_SELECT = (byte)0xa4;
        public static final byte ISO7816_READ_DATA = (byte)0xb0;
        public static final byte ISO7816_WRITE_DATA = (byte)0xd0;
    }

    /**
     * Status messages according to ISO standard and utilities
     */
    public static final class StatusMessage {
        public static final byte[] SUCCESS          = new byte[] {(byte)0x90, (byte)0x00};
        public static final byte[] SUCCESS_LC_DATA  = new byte[] {(byte)0x61, (byte)0x00};
        public static final byte[] ERR_PARAMS       = new byte[] {(byte)0x6a, (byte)0x00};
        public static final byte[] ERR_NO_DATA      = new byte[] {(byte)0x6a, (byte)0x82};
        // Custom status messages

    }

    public static final void insertStatusMessage(byte[] buffer, final byte[] statusMessage) {
        System.arraycopy(statusMessage, 0, buffer, buffer.length - 2, 2);
    }

    public static int getHeaderValue(final byte[] input, int headerField) {
        assert (headerField < (Header.DATA + 1));
        return (byte)(input[headerField] & 0xff);
    }

    /**
     * Utility method to convert a byte array to a hexadecimal string.
     *
     * @param bytes Bytes to convert
     * @return String, containing hexadecimal representation.
     */
    public static String ByteArrayToHexString(byte[] bytes) {
        final char[] hexArray = {'0','1','2','3','4','5','6','7','8','9','A','B','C','D','E','F'};
        char[] hexChars = new char[bytes.length * 2]; // Each byte has two hex characters (nibbles)
        int v;
        for (int j = 0; j < bytes.length; j++) {
            v = bytes[j] & 0xFF; // Cast bytes[j] to int, treating as unsigned value
            hexChars[j * 2] = hexArray[v >>> 4]; // Select hex character from upper nibble
            hexChars[j * 2 + 1] = hexArray[v & 0x0F]; // Select hex character from lower nibble
        }
        return new String(hexChars);
    }

    /**
     * Utility method to convert a hexadecimal string to a byte string.
     *
     * <p>Behavior with input strings containing non-hexadecimal characters is undefined.
     *
     * @param s String containing hexadecimal characters to convert
     * @return Byte array generated from input
     * @throws java.lang.IllegalArgumentException if input length is incorrect
     */
    public static byte[] HexStringToByteArray(String s) throws IllegalArgumentException {
        int len = s.length();
        if (len % 2 == 1) {
            throw new IllegalArgumentException("Hex string must have even number of characters");
        }
        byte[] data = new byte[len / 2]; // Allocate 1 byte per 2 hex characters
        for (int i = 0; i < len; i += 2) {
            // Convert each character into a integer (base-16), then bit-shift into place
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }
}
