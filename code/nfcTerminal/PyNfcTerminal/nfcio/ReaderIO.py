from io import BytesIO
import json
import logging
import struct
import time
from RSACrypto import RSACrypto

from protocol import APDU
from protocol.NfcReader import NFCReader


def hex_dump(buffer):
    """Dumps the buffer as an hex string"""
    return ' '.join(["%0.2X" % x for x in buffer])


class ReaderIO(object):
    MAX_BYTES = 4096
    MAX_CHUNK_SIZE = 125  # 250 quite stable, too

    def __init__(self, device, log=None):
        self.log = log or logging.getLogger(__name__)
        # Connection to hardware device
        self.device = device or NFCReader()

    def receive_data(self, wait_timeout=0):
        """
        Receives fragmented data messages and aggregates them to a complete data block.

        :param wait_timeout: in milliseconds. If 0, calling this function will block indefinitely.
        :return: assembled data as bytes
        """
        # Initialize data buffers
        data_in_buf = BytesIO()

        # Wait for NFC card to become ready for data transmission
        rx_buf = None
        rx_len = 0
        request_msg = APDU.build_read_message(self.MAX_CHUNK_SIZE)
        self.log.debug('Waiting for available data.')
        while rx_len <= APDU.STATUS_LEN:
            rx_buf, rx_len = self.device.transceive_message(request_msg, len(request_msg))

        # Data is ready now: aggregate fragmented data chunks
        more_data = True
        self.log.debug('Receiving data.')

        while more_data:
            mv_rx_buf = memoryview(rx_buf)
            if data_in_buf.tell() > self.MAX_BYTES:
                raise BufferError('Too much input data.')

            # Collect data input
            data_in_buf.write(mv_rx_buf[:rx_len-2])
            # Check status code at the end of the message for available data
            self.log.debug('Status byte: %s, requested: %s', hex_dump(mv_rx_buf[rx_len-2]), hex_dump(APDU.SUCCESS_LC_DATA))
            if APDU.SUCCESS_LC_DATA == mv_rx_buf[rx_len-2]:
                # More data available: fetch it
                data_size = struct.unpack_from('!B', mv_rx_buf, rx_len-1)[0]
                request_msg[APDU.LC] = min(self.MAX_CHUNK_SIZE, data_size)
                rx_buf, rx_len = self.device.transceive_message(request_msg, len(request_msg))
            else:
                more_data = False
                self.log.debug('Finished data transfer of %d bytes.', data_in_buf.tell())

        return data_in_buf.getvalue()

    def send_data(self, tx_data, wait_timeout=0):
        """
        Send tx_data as fragmented NFC messages to the target.

        This function will block as long as the whole data entity is transmitted.
        If an error occurs during transmission, an IOError will be raised.

        :param tx_data: sets the data byte array to be transmitted.
        :param wait_timeout: currently unused
        """
        # Initialize buffers
        data_out_buf = BytesIO(tx_data)
        data_len = len(tx_data)
        self.log.debug('Sending data.')

        # Build APDU write message with fragmented payload
        # Note: LC/LE contains the amount of bytes for the consecutive transmission.
        #       This enables the target to know when the transmission is finished.
        more_data = True
        while more_data:
            tx_payload = data_out_buf.read(self.MAX_CHUNK_SIZE)
            if len(tx_payload) > 0:
                # TX data to send
                remaining_bytes = data_len - data_out_buf.tell()  # available bytes in the next iteration
                tx_msg = APDU.build_write_message(min(remaining_bytes, self.MAX_CHUNK_SIZE))
                tx_msg.extend(tx_payload)
                self.log.debug('Remaining bytes for next round: %d', remaining_bytes)
                # Send fragment
                rx_buf, rx_len = self.device.transceive_message(tx_msg, len(tx_msg))
                # APDU.SUCCESS = b'\x00\x00'
                if APDU.SUCCESS != rx_buf:
                    raise IOError('Target did not accept transmitted data.')
            else:
                more_data = False

    def run(self):
        """
        Defines the logic for the communication with the target.

        IOErrors have to be handled carefully. Otherwise, the terminal application is stopped.
        :return:
        """
        crypto = RSACrypto()
        crypto.import_private_key_from_pem_file('../testResources/terminal_certs/private_key.pem')
        crypto.import_public_key_from_pem_file('../testResources/terminal_certs/public_key.pem')

        hw_activated = True
        while hw_activated:
            try:
                self.device.start_nfc_reader()

                nonce_r_S = None
                nonce_r_T = None

                """
                Protocol step 1: receive {nonce r_s, pseudo_student_id} encrypted with our T_pub key
                """
                cipher_msg_in = self.receive_data()
                self.log.info('Received raw data from client: %s', cipher_msg_in)
                # Decrypt message
                raw_msg_in = crypto.decrypt(cipher_msg_in)
                self.log.info("Decrypted msg: %s", raw_msg_in)
                # Todo: check for errors
                msg_in = json.loads(raw_msg_in.decode('utf-8'))
                nonce_r_S = msg_in['rs']

                """
                Protocol step 2: send {r_s, r_t, T} encrypted with S_pub key

                r_s: the nonce the target sent us
                r_t: a new nonce
                T: hash of our public key (does this help anything? better sign the message in addition to that)
                """
                nonce_r_T = "mysecretnoncefromTerminalrT"
                msg_out = json.dumps({
			        "type": 2,
                    "rs": nonce_r_S,
                    "rt": nonce_r_T
		        }).encode('utf-8')
                self.log.debug('Preparing for sending message: %s', msg_out)
                #msg_out = bytes(text.encode('utf-8'))
                # Encrypt with public key of mobile phone
                encrypted_msg_out = crypto.encrypt(msg_out)
                self.send_data(encrypted_msg_out)

                """
                Protocol step 3: receive {r_t, commands} encrypted with our T_pub key

                At this step, the properties freshness and confidentiality should be fulfilled.
                It should be ok to trust the target.
                """
                cipher_msg_in = self.receive_data()
                self.log.info('2. Received data from client: %s', cipher_msg_in)
                # Decrypt message
                raw_msg_in = crypto.decrypt(cipher_msg_in)
                self.log.info("Decrypted msg: %s", raw_msg_in)
                # Todo: check for errors
                msg_in = json.loads(raw_msg_in.decode('utf-8'))
                msg_r_T = msg_in['rt']
                if nonce_r_T == msg_r_T:
                    # Identical nonces: message should be fresh
                    self.log.info('Nonces identical. r_T="%s"', nonce_r_T)
                else:
                    self.log.error('Nonces r_T do not match. Replay attack?')

                self.device.shutdown_nfc_reader()

                # Wait some time before permitting a client to connect again
                # Otherwise Android tries to reconnect, but actually, it should be not necessary
                time.sleep(3)

            except (KeyboardInterrupt, SystemExit):
                hw_activated = False
            except AttributeError as e:
                self.log.error("AttributeError due to mismatch in protocol: " + str(e))
                hw_activated = True
            except IOError as e:
                self.log.error("IOError Exception: " + str(e))
                hw_activated = True
            except BufferError as e:
                self.log.error("Assuming misbehaving target. BufferError: " + str(e))
                hw_activated = True
            finally:
                self.device.shutdown_nfc_reader()
