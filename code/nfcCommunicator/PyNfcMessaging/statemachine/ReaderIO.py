from io import BytesIO
import logging
import struct


class ReaderIO(object):
    MAX_BYTES = 4096
    MAX_CHUNK_SIZE = 125  # 250 quite stable, too

    def __init__(self, log=None):
        self.log = log or logging.getLogger(__name__)
        # Raw byte buffer
        self.data_out = BytesIO()
        self.data_in = BytesIO()
        # Progress states and flags
        self.stop_communication = False
        self.conn_established = False

    def should_stop(self):
        """
        Tells whether the communication with the current device should be stopped.
        This can have different reasons like
        - an invalid message was received which can not be handled
        - the communication is finished

        :return: if true, stop the current communication. Otherwise, continue.
        """
        return self.stop_communication

    def do_IO(self, rx_buffer, rx_len):
        """ Handles incoming messages and returns an answer according to the internal state of the state machine.
        Fragmented input messages are aggregated to recover the original data.
        The same for outgoing data: the data is fragmented into chunks of MAX_CHUNK_SIZE bytes.

        For higher logic and more advanced functionality, this class should be derived.
        :param rx_buffer:
        :param rx_len:
        :return:
        """
        tx_buffer = b'\x00\x04\x00\x00\x05'
        tx_len = 5
        self.log.debug('In statemachine.')

        # Test
        # self.stop_communication = True

        return tx_buffer, tx_len
