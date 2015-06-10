"""Simple NFC card emulation"""

import logging
import ctypes
import string
import nfc

def hex_dump(string):
    """Dumps data as hexstrings"""
    return ' '.join(["%0.2X" % ord(x) for x in string])

### NFC device setup
class NFCReader(object):
    MC_AUTH_A = 0x60
    MC_AUTH_B = 0x61
    MC_READ = 0x30
    MC_WRITE = 0xA0
    card_timeout = 10

    def __init__(self, logger):
        self.__context = None
        self.__device = None
        self.log = logger

        self._card_present = False
        self._card_last_seen = None
        self._card_uid = None
        self._clean_card()

        # Prepare NFC reader hardware for card emulation
        card = nfc.nfc_iso14443a_info() # type: target_info
        card.abtAtqa[0] = 0x00
        card.abtAtqa[1] = 0x04
        atqaSet = (ctypes.c_uint8 * 2).from_buffer(card.abtAtqa)
        atqaSet[:] = [0x00, 0x04]
        # card.abtAtqa[:] = [0x00, 0x04] # also seems to work directly
        print(atqaSet[:])

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

        self.__modulation = nfc.nfc_modulation()
        self.__modulation.nmt = nfc.NMT_ISO14443A
        self.__modulation.nbr = nfc.NBR_UNDEFINED

        self.__target = nfc.nfc_target()
        self.__target.nm = self.__modulation
        #self.__target.nti = self.__target_info
        self.__target.nti.nai = card

    def run(self):
        """Starts the looping thread"""
        self.__context = ctypes.pointer(nfc.nfc_context())
        print("NFC_init")
        nfc.nfc_init(ctypes.byref(self.__context))
        print("NFC_init finished")
        loop = True
        try:
            self._clean_card()
            conn_strings = (nfc.nfc_connstring * 1)()
            devices_found = nfc.nfc_list_devices(self.__context, ctypes.byref(conn_strings), 10)
            if devices_found >= 1:
                print("Device found!")
            print("NFC_open")
            self.__device = nfc.nfc_open(self.__context, conn_strings[0])
            print("NFC_open finsished")
            # print self.__device.last_error
            try:

                while loop:
                    self._message_loop()
            finally:
                nfc.nfc_close(self.__device)
        except (KeyboardInterrupt, SystemExit):
            loop = False
        except IOError as e:
            self.log("Exception: " + str(e))
            loop = True
        # except Exception, e:
        # loop = True
        #    print "[!]", str(e)
        finally:
            nfc.nfc_exit(self.__context)
            self.log("NFC Clean shutdown called")
        return loop

    @staticmethod
    def _sanitize(bytesin):
        """Returns guaranteed ascii text from the input bytes"""
        return "".join([x if 0x7f > ord(x) > 0x1f else '.' for x in bytesin])

    @staticmethod
    def _hashsanitize(bytesin):
        """Returns guaranteed hexadecimal digits from the input bytes"""
        return "".join([x if x.lower() in 'abcdef0123456789' else '' for x in bytesin])


    def _message_loop(self):
        """Starts a loop that simulates a smartcard"""
        #res = nfc.nfc_initiator_poll_target(self.__device, self.__modulations, len(self.__modulations), 10, 2,
        #                                    ctypes.byref(nt))
        rxBuf = (ctypes.c_uint8 * 250)()
        print("nfc_target_init")
        res = nfc.nfc_target_init(self.__device, ctypes.byref(self.__target), ctypes.pointer(rxBuf), 250, 0)
        print("Finsihed nfc_target_init")
        print(rxBuf[:])
        print(res)

        # Receive first APDU message
        print("Receive first string")
        cRx = nfc.nfc_target_receive_bytes(self.__device, ctypes.byref(rxBuf), 250, 0)
        print(rxBuf[:])
        print(cRx)

        # Send ok
        txBuf = (ctypes.c_uint8 * 2)()
        txBuf[:] = [0x90, 0x00]
        cTx = nfc.nfc_target_send_bytes(self.__device, ctypes.byref(txBuf), 2, 0)
        print("Send bytes: %d" % cTx)

        # print "RES", res
        # if res < 0:
        #     raise IOError("NFC Error whilst polling")
        # elif res >= 1:
        #     uid = None
        #     if nt.nti.nai.szUidLen == 4:
        #         uid = "".join([chr(nt.nti.nai.abtUid[i]) for i in range(4)])
        #     if uid:
        #         if not ((self._card_uid and self._card_present and uid == self._card_uid) and \
        #                             time.mktime(time.gmtime()) <= self._card_last_seen + self.card_timeout):
        #             self._setup_device()
        #             self.read_card(uid)
        #     self._card_uid = uid
        #     self._card_present = True
        #     self._card_last_seen = time.mktime(time.gmtime())
        # else:
        #     self._card_present = False
        #     self._clean_card()

    def _initiate_connection(self):
        """
        Polls for a NFC candidate
        TODO
        """

    def _clean_card(self):
        self._card_uid = None

if __name__ == '__main__':
    logger = logging.getLogger("cardhandler").info
    while NFCReader(logger).run():
        pass
