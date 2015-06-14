"""Simple NFC card emulation"""

import logging
import ctypes

import nfc
from nfc_error import HWError
from nfc_statemachine import CommStateMachine

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
                self.log.info('Wait for NFC initiator for 5s')
                # NFC abstraction: target_init
                connection_res = nfc.nfc_target_init(self.__device,
                                                     ctypes.byref(self.__target),
                                                     ctypes.pointer(self.__rx_msg),
                                                     self.ISO7816_SHORT_APDU_MAX_LEN, 5000)
                if connection_res >= 0:
                    self.log.debug(self.__rx_msg[:connection_res])
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
        """Starts a loop that simulates a smart card"""
        stm = CommStateMachine()

        loop = True
        while loop:
            # Receive APDU message
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


if __name__ == '__main__':
    logging.basicConfig(format='[%(levelname)s:%(name)s:%(funcName)s] %(message)s', level=logging.DEBUG)
    logger = logging.getLogger(__name__)
    # Do not pass logger object to generate unique names per module
    reader = NFCReader()
    while reader.run():
        pass
