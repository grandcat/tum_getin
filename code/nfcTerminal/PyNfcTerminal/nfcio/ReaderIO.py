import base64
from io import BytesIO
import json
import logging
import os
import struct
import time
from RSACrypto import RSACrypto
from backend.BackendConnection import Backend

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
        self.log.debug('Receiving data ...')

        while more_data:
            mv_rx_buf = memoryview(rx_buf)
            if data_in_buf.tell() > self.MAX_BYTES:
                raise BufferError('Too much input data.')

            # Collect data input
            data_in_buf.write(mv_rx_buf[:rx_len-2])
            # self.log.debug('Status byte: %s, requested: %s', hex_dump(mv_rx_buf[rx_len-2]), hex_dump(APDU.SUCCESS_LC_DATA))
            # Check status code at the end of the message for available data
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
                remaining_bytes = data_len - data_out_buf.tell()  # available bytes for the next iteration
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

        Needs to be overwritten to provide logic.
        """
        raise NotImplementedError('Overwrite with function to implement the ReaderIO logic.')
