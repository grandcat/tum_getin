"""
NFC card reader
"""

import ctypes
import logging
import time

import nfc

from protocol import APDU
from protocol.NfcError import HWError, TargetLost


def hex_dump(buffer):
    """Dumps the buffer as an hex string"""
    return ' '.join(["%0.2X" % x for x in buffer])

class NFCReader(object):
    """
    Short APDU max transceive length
    ISO7816_SHORT_APDU_MAX_DATA_LEN + APDU_COMMAND_HEADER + APDU_RESPONSE_TRAILER
    """
    ISO7816_SHORT_APDU_MAX_LEN = 256 + 4 + 2
    """
    Time between two consecutive probes for an NFC emulated card in range
    """
    PROBE_DEVICE_INTERVAL = 1.2

    # SELECT_AID = b'\x00\xA4\x04\x00\x07\xF0\x74\x75\x6D\x67\x65\x74'
    SELECT_AID = b'\x00\xA4\x04\x00\x0A\xF0' + b'tumgetin\x02'

    def __init__(self, log=None):
        self.name = 'nfc.hw'
        self.log = log or logging.getLogger(__name__)
         # NFC hardware configuration
        self.__context = None
        self.__device = None
        self.__target = None
        self.__modulation = None
        self.running = False
         # Raw send buffer and raw receive buffer for exchanging one APDU message over NFC
        self.__tx_msg = (ctypes.c_uint8 * self.ISO7816_SHORT_APDU_MAX_LEN)()
        self.__rx_msg = (ctypes.c_uint8 * self.ISO7816_SHORT_APDU_MAX_LEN)()

        self.init_card_reader_mode()
        self.__conn_strings = (nfc.nfc_connstring * 1)()


    @staticmethod
    def _sanitize(bytesin):
        """Returns guaranteed ascii text from the input bytes"""
        return "".join([x if 0x7f > ord(x) > 0x1f else '.' for x in bytesin])

    @staticmethod
    def _hashsanitize(bytesin):
        """Returns guaranteed hexadecimal digits from the input bytes"""
        return "".join([x if x.lower() in 'abcdef0123456789' else '' for x in bytesin])

    # def run(self):
    #     """
    #     Connects to the NFC hardware and starts messaging with emulating smart card
    #     """
    #     connection_loop = True
    #     while connection_loop:
    #         try:
    #             self.log.debug('NFC init')
    #             nfc.nfc_init(ctypes.byref(self.__context))
    #             # Select HW reader
    #             devices_found = nfc.nfc_list_devices(self.__context, ctypes.byref(self.__conn_strings), 1)
    #             if devices_found != 1:
    #                 raise HWError('No suitable NFC device found. Check your libnfc config and hardware.')
    #
    #             # NFC abstraction: open
    #             self.__device = nfc.nfc_open(self.__context, self.__conn_strings[0])
    #             self.log.debug('NFC_open finished')
    #
    #             # Start reader as initiator with RF field, but without infinite polling to reduce CPU usage
    #             result = nfc.nfc_initiator_init(self.__device)
    #             if result < 0:
    #                 raise HWError('Could not start or configure reader as NFC initiator properly.')
    #             self.log.debug('Started NFC initiator.')
    #
    #             while connection_loop:
    #                 self.log.info('Polling for NFC target...')
    #                 result += nfc.nfc_device_set_property_bool(self.__device, nfc.NP_INFINITE_SELECT, False)
    #                 found_card = nfc.nfc_initiator_select_passive_target(self.__device,
    #                                                                      self.__modulation,
    #                                                                      None,
    #                                                                      0,
    #                                                                      ctypes.byref(self.__target))
    #                 # Wait until target was found
    #                 if found_card > 0:
    #                     # Got target
    #                     self.log.debug('Target with ISO14443A modulation found sending at baudrate type %d.',
    #                                    self.__target.nm.nbr)
    #                     # Init new state machine and start message exchange until target leaves
    #                     self.send_uid()
    #                     # time.sleep(4)
    #                     # self.send_uid()
    #                     stm = ReaderIO()
    #                     self._message_loop(stm)
    #
    #                 else:
    #                     # No target found
    #                     self.log.debug('No target found. Sleeping for 1 s.')
    #                     time.sleep(self.PROBE_INTERVAL)
    #
    #         except (KeyboardInterrupt, SystemExit):
    #             connection_loop = False
    #         except HWError as e:
    #             self.log.error("Hardware exception: " + str(e))
    #             connection_loop = False
    #         except IOError as e:
    #             self.log.error("IOError exception: " + str(e))
    #             connection_loop = True
    #
    #         finally:
    #             nfc.nfc_close(self.__device)
    #             nfc.nfc_exit(self.__context)
    #             self.log.info('NFC clean shutdown')
    #     # Connection loop: repeat

    def init_card_reader_mode(self):
        """
        Prepares NFC reader hardware for interfacing with smart cards
        """
        # Considerable target devices : mobile phone emulating smart card
        if self.__target is None:
            self.__target = nfc.nfc_target()

        if self.__context is None:
            self.__context = ctypes.pointer(nfc.nfc_context())

        # Modulation properties
        if self.__modulation is None:
            self.__modulation = nfc.nfc_modulation()

        self.__modulation.nmt = nfc.NMT_ISO14443A
        self.__modulation.nbr = nfc.NBR_106

    def start_nfc_reader(self):
        connection_loop = True
        try:
            self.log.debug('NFC init')
            nfc.nfc_init(ctypes.byref(self.__context))
            # Select HW reader
            devices_found = nfc.nfc_list_devices(self.__context, ctypes.byref(self.__conn_strings), 1)
            if devices_found != 1:
                raise HWError('No suitable NFC device found. Check your libnfc config and hardware.')

            # NFC abstraction: open
            self.__device = nfc.nfc_open(self.__context, self.__conn_strings[0])
            self.log.debug('NFC_open finished')

            # Start reader as initiator with RF field, but without infinite polling to reduce CPU usage
            result = nfc.nfc_initiator_init(self.__device)
            if result < 0:
                raise HWError('Could not start or configure reader as NFC initiator properly.')
            self.log.debug('Started NFC initiator.')

            while connection_loop:
                self.log.info('Polling for NFC target...')
                result += nfc.nfc_device_set_property_bool(self.__device, nfc.NP_INFINITE_SELECT, False)
                found_card = nfc.nfc_initiator_select_passive_target(self.__device,
                                                                     self.__modulation,
                                                                     None,
                                                                     0,
                                                                     ctypes.byref(self.__target))
                # Wait until target was found
                if found_card > 0:
                    # Got target
                    self.log.debug('Connection established with ISO14443A modulation and baudrate type %d.',
                                   self.__target.nm.nbr)
                    # Send UID to authenticate reader against Android device
                    # -> it will wait until Android's NFC service is ready
                    self.send_uid()
                    # Finish setup step
                    self.running = True
                    connection_loop = False

                else:
                    # No target found
                    self.log.debug('No target found. Sleeping for 1 s.')
                    time.sleep(1.2)

        except (KeyboardInterrupt, SystemExit):
            connection_loop = False
        except HWError as e:
            self.log.error("Hardware exception: " + str(e))
            connection_loop = False
        except IOError as e:
            self.log.error("IOError exception: " + str(e))
            connection_loop = True

        return connection_loop

    def shutdown_nfc_reader(self):
        if self.running:
            self.running = False
            try:
                nfc.nfc_close(self.__device)
                nfc.nfc_exit(self.__context)
            finally:
               self.log.info('NFC clean shutdown')

    def send_uid(self):
        """Send UID to allow the Android app to recognize our reader"""
        tx_buf = self.SELECT_AID
        connected = False
        while not connected:
            rx_buf, rx_len = self.transceive_message(tx_buf, len(tx_buf))
            if APDU.SUCCESS == rx_buf:
                # Android device should be ready now for further communication
                connected = True
            elif APDU.ERR_NOT_READY == rx_buf:
                # Wait some time until Android NFC service is ready
                time.sleep(0.5)
                connected = False
                self.log.debug('Android NFC service not ready, waiting...')
            else:
                raise IOError('Unknown device.')


    def transceive_message(self, tx_buf, tx_len, timeout=0):
        assert self.__device != None
        # Prepare message for sending
        assert tx_len > 0
        self.__tx_msg[:tx_len] = tx_buf
        self.log.debug('Send [%d bytes]: %s', tx_len, hex_dump(tx_buf[:]))
        # NFC hardware IO
        rx_len = nfc.nfc_initiator_transceive_bytes(self.__device,
                                                    ctypes.byref(self.__tx_msg),
                                                    tx_len,
                                                    ctypes.byref(self.__rx_msg),
                                                    self.ISO7816_SHORT_APDU_MAX_LEN,
                                                    0)
        # Receiving message
        if rx_len > 0:
            rx_buf = bytes((ctypes.c_char * rx_len).from_buffer(self.__rx_msg))
            self.log.debug('Receive [%d bytes]: %s', rx_len, hex_dump(rx_buf[:rx_len]))
        elif nfc.NFC_ERFTRANS == rx_len:
            raise TargetLost('Lost link to target.')
        elif nfc.NFC_EINVARG == rx_len:
            # Invalid argument seems to be a keyboard interrupt
            raise KeyboardInterrupt()
        else:
            # No valid response: link is probably broken
            raise IOError("Invalid or no reply from target. libnfc type: " + str(rx_len))

        return rx_buf, rx_len

    def _message_loop(self, stm):
        """Starts a loop that simulates an NFC reader to interact with a smart card"""

        loop = True
        rx_buf = []
        rx_len = 0
        while loop:
            # State machine for communication with end device
            tx_buf, tx_len = stm.do_IO(rx_buf, rx_len)
            if stm.should_stop():
                return

            # Prepare message for sending
            assert tx_len > 0
            self.__tx_msg[:tx_len] = tx_buf
            self.log.debug('Send [%d bytes]: %s', tx_len, hex_dump(tx_buf[:]))
            # NFC hardware IO
            rx_len = nfc.nfc_initiator_transceive_bytes(self.__device,
                                                        ctypes.byref(self.__tx_msg),
                                                        tx_len,
                                                        ctypes.byref(self.__rx_msg),
                                                        self.ISO7816_SHORT_APDU_MAX_LEN,
                                                        0)
            if rx_len < 0:
                # Error occurred (e.g., target was removed from the carrier field) --> restart
                self.log.warning('Error %d while transceiving data.', rx_len)
                return
            # Receiving message
            rx_buf = bytes((ctypes.c_char * rx_len).from_buffer(self.__rx_msg))
            self.log.debug('Receive [%d bytes]: %s', rx_len, hex_dump(rx_buf[:rx_len]))
