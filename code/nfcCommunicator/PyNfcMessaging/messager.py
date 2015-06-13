"""Simple NFC card emulation"""

import logging
import nfc_error
from nfc_error import HWError
import ctypes
import string
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
        except nfc_error.HWError as e:
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
                self.log.warning('Error %i in nfc_target_receive_bytes', rx_len)
                return

            rx_buf = (ctypes.c_char * rx_len).from_buffer(self.__rx_msg)
            self.log.info('Receive [%d bytes]: %s', rx_len, hex_dump(rx_buf[:]))

            # State machine for communication with end device
            tx_buf, tx_len = stm.doIO(rx_buf, rx_len)
            loop = not stm.shouldStop()

            # Transmit message
            if tx_len > 0:
                # TODO: improve python->ctype conversion
                self.__tx_msg[:tx_len] = tx_buf
                tx_len = nfc.nfc_target_send_bytes(self.__device,
                                                   ctypes.byref(self.__tx_msg),
                                                   tx_len, 0)
                self.log.info('Send [%d bytes]: %s', tx_len, hex_dump(tx_buf[:]))

            # loop = False

    def _clean_card(self):
        self._card_uid = None


class CommStateMachine:

    def __init__(self, log=None):
        self.log = log or logging.getLogger(__name__)
        self.stop_communication = False

    def shouldStop(self):
        """
        Tells whether the communication with the current device should be ended.
        This can have different reasons like
        - an invalid message was received which can not be handled
        - the communication is finished

        :return: if true, stop the current communication. Otherwise, continue.
        """
        return self.stop_communication

    def doIO(self, rx_buffer, rx_len):
        """ Handles incoming messages and dispatches a process to generate an answer
        :param rx_buffer:
        :param rx_len:
        :return:
        """
        tx_buffer = []
        tx_len = 0

        

        tx_buffer += [0x90, 0x00]

        tx_len = len(tx_buffer)
        return tx_buffer, tx_len


if __name__ == '__main__':
    logging.basicConfig(level=logging.DEBUG)
    logger = logging.getLogger(__name__)

    reader = NFCReader(logger)
    while reader.run():
        pass
