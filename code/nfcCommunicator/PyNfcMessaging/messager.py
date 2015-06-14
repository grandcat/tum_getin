"""Simple NFC card emulation"""

import logging
import struct
import time

from nfc_error import HWError
import ctypes
import nfc

def hex_dump(buffer):
    """Dumps the buffer as an hex string"""
    return ' '.join(["%0.2X" % x for x in buffer])

# NFC device setup
class NFCReader(object):
    """Short APDU max transceive length: ISO7816_SHORT_APDU_MAX_DATA_LEN + APDU_COMMAND_HEADER + APDU_RESPONSE_TRAILER """
    ISO7816_SHORT_APDU_MAX_LEN = 256 + 4 + 2

    def __init__(self, log=None):
        self.log = log or logging.getLogger(__name__)
        # NFC hardware fields
        self.__context = None
        self.__device = None
        self.__target = None
        # Send and receive buffer for exchanging one APDU message over NFC
        self.__tx_msg = (ctypes.c_uint8 * self.ISO7816_SHORT_APDU_MAX_LEN)()
        self.__rx_msg = (ctypes.c_uint8 * self.ISO7816_SHORT_APDU_MAX_LEN)()

        self._card_present = False
        self._card_last_seen = None
        self._card_uid = None
        self._clean_card()

        # Init
        self.init_card_emulation_target()

    @staticmethod
    def _sanitize(bytesin):
        """Returns guaranteed ascii text from the input bytes"""
        return "".join([x if 0x7f > ord(x) > 0x1f else '.' for x in bytesin])


    @staticmethod
    def _hashsanitize(bytesin):
        """Returns guaranteed hexadecimal digits from the input bytes"""
        return "".join([x if x.lower() in 'abcdef0123456789' else '' for x in bytesin])


    def init_card_emulation_target(self):
        """
        Prepares NFC reader hardware for card emulation
        """
        if self.__target == None:
            self.__target = nfc.nfc_target()

        # Modulation properties
        modulation = self.__target.nm       # access by reference
        modulation.nmt = nfc.NMT_ISO14443A
        modulation.nbr = nfc.NBR_UNDEFINED

        # Target info: card emulation
        card = nfc.nfc_iso14443a_info() # type: target_info
        card.abtAtqa[:] = [0x00, 0x04]
        #atqaSet = (ctypes.c_uint8 * 2).from_buffer(card.abtAtqa)
        #atqaSet[:] = [0x00, 0x04]
        # card.abtAtqa[:] = [0x00, 0x04] # also seems to work directly
        print(card.abtAtqa[:])

        uid = "\x08\x00\xb0\xfe"
        for i in range(4):
            card.abtUid[i] = ord(uid[i])
        card.szUidLen = 4
        #card.abtAtqa = [0x00, 0x04]

        card.btSak = 0x20

        atsSet = (ctypes.c_uint8 * 4).from_buffer(card.abtAts)
        atsSet[:] = [0x75, 0x33, 0x92, 0x03]
        print(card.abtAts[:])
        card.szAtsLen = 4

        # Assign target_info for card emulation
        # self.__target.nm = modulation
        #self.__target.nti = self.__target_info
        self.__target.nti.nai = card


    def run(self):
        """
        Connects with the NFC reader and starts messaging with NFC devices
        """
        self.__context = ctypes.pointer(nfc.nfc_context())
        self.log.debug('NFC init')
        nfc.nfc_init(ctypes.byref(self.__context))
        connection_loop = True
        try:
            self._clean_card()
            conn_strings = (nfc.nfc_connstring * 1)()
            devices_found = nfc.nfc_list_devices(self.__context, ctypes.byref(conn_strings), 1)
            if devices_found != 1:
                raise HWError('No NFC device found. Check your libnfc config and hardware.')

            self.log.debug('NFC open')
            # NFC abstraction: open
            self.__device = nfc.nfc_open(self.__context, conn_strings[0])
            self.log.debug('NFC_open finished')
            # self.log.debug(self.__device.last_error)

            while connection_loop:
                # Reset all state variables
                self.log.debug('Wait for NFC initiator for 5s')
                # NFC abstraction: target_init
                connection_res = nfc.nfc_target_init(self.__device,
                                                     ctypes.byref(self.__target),
                                                     ctypes.pointer(self.__rx_msg),
                                                     self.ISO7816_SHORT_APDU_MAX_LEN, 5000)
                print(connection_res)
                if connection_res >= 0:
                    self.log.debug(self.__rx_msg[:])

                    # Start message exchange state machine
                    self._message_loop()

                elif connection_res != nfc.NFC_ETIMEOUT:
                    self.log.warning('nfc_target_init: got error %i', connection_res)

        except (KeyboardInterrupt, SystemExit):
            connection_loop = False
        except HWError as e:
            self.log.error("Hardware exception: " + str(e))
            connection_loop = False
        except IOError as e:
            self.log.error("IOError Exception: " + str(e))
            connection_loop = True

        finally:
            nfc.nfc_close(self.__device)
            nfc.nfc_exit(self.__context)
            self.log.info('NFC clean shutdown')
        return connection_loop


    def _message_loop(self):
        """Starts a loop that simulates a smartcard"""
        stm = CommStateMachine()

        loop = True
        while loop:
            # Receive APDU message
            print("Receive first string")
            rx_len = nfc.nfc_target_receive_bytes(self.__device,
                                                  ctypes.byref(self.__rx_msg),
                                                  self.ISO7816_SHORT_APDU_MAX_LEN, 0)
            if (rx_len < 0):
                # Error occurred (e.g., target was removed from the carrier field) --> restart
                self.log.warning('Error %d in nfc_target_receive_bytes', rx_len)
                return

            rx_buf = bytes((ctypes.c_char * rx_len).from_buffer(self.__rx_msg))
            self.log.debug('Receive [%d bytes]: %s', rx_len, hex_dump(rx_buf[:]))

            # State machine for communication with end device
            tx_buf, tx_len = stm.do_IO(rx_buf, rx_len)
            loop = not stm.should_stop()
            # time.sleep(1) # testing tolerance of timeout value

            # Transmit message
            if tx_len > 0:
                # TODO: improve python->ctype conversion
                self.__tx_msg[:tx_len] = tx_buf
                tx_len = nfc.nfc_target_send_bytes(self.__device,
                                                   ctypes.byref(self.__tx_msg),
                                                   tx_len, 0)
                self.log.debug('Send [%d bytes]: %s', tx_len, hex_dump(tx_buf[:]))
                if (tx_len < 0):
                    # Error occurred (e.g., target was removed from the carrier field) --> restart
                    self.log.warning('Error %d in nfc_target_send_bytes', tx_len)
                    return

            # loop = False

    def _clean_card(self):
        self._card_uid = None


class CommStateMachine:
    IDENTIFICATION = b'tumgetin\x01' # hex: 74 75 6D 67 65 74 69 6E 01
    MAX_BYTES = 4096
    MAX_CHUNK_SIZE = 250

    data_out = bytearray(b'\xbe\xef\xaf\xfe' * 128 * 2)
    data_out.extend([0x11, 0x22, 0x33])
    data_out_ready = False  # If true, buffer is complete and ready for transmission (NOTE: must be atomic)
    data_out_offset = 0
    data_in = bytearray()
    data_in_ready = False
    data_in_offset = 0

    def __init__(self, log=None):
        self.log = log or logging.getLogger(__name__)
        # Progress states and flags
        self.stop_communication = False
        self.conn_established = False

    def should_stop(self):
        """
        Tells whether the communication with the current device should be ended.
        This can have different reasons like
        - an invalid message was received which can not be handled
        - the communication is finished

        :return: if true, stop the current communication. Otherwise, continue.
        """
        return self.stop_communication

    def do_IO(self, rx_buffer, rx_len):
        """ Handles incoming messages and dispatches a process to generate an answer
        :param rx_buffer:
        :param rx_len:
        :return:
        """
        # Default transmission buffer
        tx_buffer = []
        tx_len = 0

        # TODO: remove the following test
        self.data_out_ready = True

        if rx_len > 0:
            action = rx_buffer[APDU.INS]

            if APDU.ISO7816_SELECT == action:
                # Check for supported target device running our App
                if rx_buffer[APDU.P1] == 0x04 and rx_buffer[APDU.P2] == 0x00:
                    # Select by name
                    if rx_buffer[APDU.DATA:] == self.IDENTIFICATION:
                        self.conn_established = True
                        return APDU.msg_success()
                    else:
                        # No expected client --> Goodbye
                        self.log.warning('Refusing unknown client')
                        self.stop_communication = True
                        return APDU.msg_err_wrong_data()

            elif (APDU.ISO7816_READ_DATA == action) and self.conn_established:
                # Client asks to receive available data
                if self.data_out_ready:
                    # Push next chunk of data to client
                    return self._push_data_to_client(rx_buffer)
                else:
                    # No data here right now
                    return APDU.msg_err_no_data()

            elif (APDU.CUSTOM_DATA_SIZE == action) and self.conn_established:
                if APDU.ISO7816_READ_DATA == rx_buffer[APDU.P1]:
                    # Client requests length of available data
                    return self._push_data_out_size(rx_buffer)

                elif APDU.ISO7816_WRITE_DATA == rx_buffer[APDU.P1]:
                    # Client requests amount of available data
                    pass
                else:
                    return APDU.msg_err_param()

        # Default fallback to keep the connection alive
        return APDU.msg_success()

    def _push_data_out_size(self, rx_buffer):
        if not self.data_out_ready:
            return APDU.msg_err_no_data()

        elif 0x02 == rx_buffer[APDU.LC]:
            # Request: size of available data
            data_size = len(self.data_out)
            # Answer: P1 * 256 + P2 encodes size
            response = struct.pack('!H2s', data_size, APDU.SUCCESS)  # Put as unsigned short with 2 bytes
            return response, 4

        else:
            return APDU.msg_err_param()

    def _push_data_to_client(self, rx_buffer):
        """Read data_out bytearray and transmit fragmented packets to client"""

        # Unpack starting from P1: [ .. | P1 | P2 | LC | .. ]
        #                                 ^offset^  ^requested_bytes
        offset, requested_bytes = struct.unpack_from('!HB', rx_buffer, APDU.P1)
        requested_bytes = min(requested_bytes, self.MAX_CHUNK_SIZE)

        # Requested data has to be within bounds of data_out buffer
        if (offset + requested_bytes) > len(self.data_out): # TODO: check for requested_bytes > 0
            return APDU.msg_err_param()

        self.log.debug('_push_data_to_client: offset %d, available %d', offset, len(self.data_out))
        buf_out = bytearray(self.data_out[offset:offset+requested_bytes])
        buf_out.extend(APDU.SUCCESS)

        return buf_out, len(buf_out)

class APDU:
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

    # Status messages
    SUCCESS = b'\x90\x00'
    ERR_PARAMS = b'\x6a\x00'
    ERR_WRONG_DATA = b'\x6a\x80'
    ERR_NO_DATA = b'\x6a\x82'

    @staticmethod
    def msg_success():
        return (APDU.SUCCESS, 2)

    @staticmethod
    def msg_success_with_data_ready(available_bytes):
        buf = [0x61] + [available_bytes]
        return (buf, 2)

    @staticmethod
    def msg_err_param():
        return (APDU.ERR_PARAMS, 2)

    @staticmethod
    def msg_err_wrong_data():
        return (APDU.ERR_WRONG_DATA, 2)

    @staticmethod
    def msg_err_no_data():
        return (APDU.ERR_NO_DATA, 2)


if __name__ == '__main__':
    logging.basicConfig(level=logging.DEBUG)
    logger = logging.getLogger(__name__)

    reader = NFCReader(logger)
    while reader.run():
        pass
