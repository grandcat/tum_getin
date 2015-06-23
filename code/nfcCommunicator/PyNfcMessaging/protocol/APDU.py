class APDU:
    HEADER_LEN = 5
    STATUS_LEN = 2
    # C-APDU offsets for header and payload
    # [CLA | INS | P1 | P2 | LC | DATA]
    CLA  = 0    # Class
    INS  = 1    # Instruction: action defined by ISO7816
    P1   = 2    # Parameter 1
    P2   = 3    # Parameter 2
    LC   = 4    # Length command LC or LE
    DATA = 5    # Payload
    # ISO7816-4 and custom instructions
    ISO7816_SELECT      = 0xA4
    ISO7816_READ_DATA   = 0xB0
    ISO7816_WRITE_DATA  = 0xD0
    CUSTOM_DATA_SIZE    = 0xCB  # if P1 = 0xB0: read data size, if P1 = OxD0: write data size
    # APDU status identifiers
    SUCCESS_LC_DATA     = b'\x61'

    # APDU status messages
    SUCCESS                 = b'\x90\x00'
    ERR_PARAMS              = b'\x6a\x00'
    ERR_WRONG_DATA          = b'\x6a\x80'
    ERR_NO_DATA             = b'\x6a\x82'
    ERR_NOT_ENOUGH_SPACE    = b'\x6a\x84'
    # Custom status messages
    ERR_NOT_READY           = b'\x6a\x90'

    @staticmethod
    def build_read_message(chunk_size):
        assert chunk_size < 256
        buf = bytearray(APDU.HEADER_LEN)
        buf[APDU.INS] = APDU.ISO7816_READ_DATA
        buf[APDU.LC] = chunk_size
        return buf

    @staticmethod
    def build_write_message(next_fragment_size):
        assert next_fragment_size < 256
        buf = bytearray(APDU.HEADER_LEN)
        buf[APDU.INS] = APDU.ISO7816_WRITE_DATA
        buf[APDU.LC] = next_fragment_size
        return buf

    @staticmethod
    def msg_success():
        return APDU.SUCCESS, 2

    @staticmethod
    def msg_success_with_data_ready(available_bytes):
        assert available_bytes < 256
        buf = b'\x61' + int.to_bytes(available_bytes, 1, byteorder='big')
        return buf, 2

    @staticmethod
    def msg_err_param():
        return APDU.ERR_PARAMS, 2

    @staticmethod
    def msg_err_wrong_data():
        return APDU.ERR_WRONG_DATA, 2

    @staticmethod
    def msg_err_no_data():
        return APDU.ERR_NO_DATA, 2

    @staticmethod
    def msg_err_not_enough_space():
        return APDU.ERR_NOT_ENOUGH_SPACE, 2

    @staticmethod
    def msg_err_not_ready():
        return APDU.ERR_NOT_READY, 2
