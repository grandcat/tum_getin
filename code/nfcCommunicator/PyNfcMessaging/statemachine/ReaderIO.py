from io import BytesIO
import logging
import struct

from protocol import APDU
from nfc_reader import NFCReader


def hex_dump(buffer):
    """Dumps the buffer as an hex string"""
    return ' '.join(["%0.2X" % x for x in buffer])


class ReaderIO(object):
    MAX_BYTES = 4096
    MAX_CHUNK_SIZE = 125  # 250 quite stable, too

    def __init__(self, device, log=None):
        self.log = log or logging.getLogger(__name__)
        # Hardware device
        self.device = device or NFCReader()
        # Communication state
        self.stop_communication = False
        self.conn_established = False
        # Activity
        self.current_activity = 0  # No activity by default

    def receive_data(self, wait_timeout=0):
        """
        Receives fragmented data messages and aggregates them to a complete data block.

        :param wait_timeout: in milliseconds. If 0, calling this function will block indefinitely.
        :return: received data item
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
        # Reset read position if buffer is reused
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
        self.device.start_nfc_reader()

        msg_in = self.receive_data()
        self.log.info('Received data from client: %s', msg_in)

        text = 'Da stimme ich zu!\n' \
               'Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ' \
               'ut labore et dolore magna aliquyam erat, sed diam voluptua. At vero eos et accusam et justo duo ' \
               'dolores et ea rebum. Stet clita kasd gubergren, no sea takimata sanctus est Lorem ipsum dolor ' \
               'sit amet. Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor ' \
               'invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. At vero eos et accusam et ' \
               'justo duo dolores et ea rebum. Stet clita kasd gubergren, no sea takimata sanctus est Lorem ipsum ' \
               'dolor sit amet. Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod ' \
               'tempor invidunt ut labore et dolore magna aliquyam'
        msg_out = bytes(text.encode('utf-8'))
        self.send_data(msg_out)

        self.device.shutdown_nfc_reader()

    def should_stop(self):
        """
        Tells whether the communication with the current device should be stopped.
        This can have different reasons like
        - an invalid message was received which can not be handled
        - the communication is finished

        :return: if true, stop the current communication. Otherwise, continue.
        """
        return self.stop_communication
