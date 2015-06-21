import logging
import queue
import struct

class CardEmulation:
    IDENTIFICATION = b'tumgetin\x01' # hex: 74 75 6D 67 65 74 69 6E 01
    MAX_BYTES = 4096
    MAX_CHUNK_SIZE = 125 # 250 quite stable, too

    # data_out = bytearray(b'\xbe\xef\xaf\xfe' * 128)
    # data_out.extend([0x11, 0x22, 0x33])
    data_out = bytearray()
    data_out_ready = False  # If true, buffer is complete and ready for transmission (NOTE: must be atomic)
    data_out_offset = 0     # For lazy client that doesn't want to index the data, we do it =)
    data_in = bytearray()
    data_in_ready = False
    data_in_size = 0
    # bytearray for input handles the offset itself, but needs a limit

    def __init__(self, queue_data_in, queue_data_out, log=None):
        self.log = log or logging.getLogger(__name__)
        # Progress states and flags
        self.stop_communication = False
        self.conn_established = False
        # IO buffer interface
        self.q_data_in = queue_data_in
        self.q_data_out = queue_data_out

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
        if rx_len > 0:
            action = rx_buffer[APDU.INS]

            # TODO: try, expect for malformed user input tries to kill our reader
            if APDU.ISO7816_SELECT == action:
                # Check for supported target device running our App
                if rx_buffer[APDU.P1] == 0x04 and rx_buffer[APDU.P2] == 0x00:
                    # Select by name
                    if rx_buffer[APDU.DATA:] == self.IDENTIFICATION:
                        self.conn_established = True
                        self.log.info('Valid client connected')
                        # TODO: remove the following test
                        self.data_in_ready = True
                        # END Test
                        return APDU.msg_success()
                    else:
                        # No expected client --> Goodbye
                        self.log.warning('Refusing unknown client')
                        self.stop_communication = True
                        return APDU.msg_err_wrong_data()

            elif (APDU.ISO7816_READ_DATA == action) and self.conn_established:
                # Client asks to receive available data
                return self._push_data_to_client(rx_buffer)

            elif (APDU.ISO7816_WRITE_DATA == action) and self.conn_established:
                # Client sends data to reader
                return self._get_data_from_client(rx_buffer)

            elif (APDU.CUSTOM_DATA_SIZE == action) and self.conn_established:
                if APDU.ISO7816_READ_DATA == rx_buffer[APDU.P1]:
                    # Client requests length of available data, check data_out queue before for ready data
                    self._try_fetch_msg_from_data_out_queue()
                    return self._push_data_out_size(rx_buffer)

                elif APDU.ISO7816_WRITE_DATA == rx_buffer[APDU.P1]:
                    # Client sets length of data he wants to send to the reader
                    return self._get_data_in_size(rx_buffer)
                else:
                    return APDU.msg_err_param()

            self._try_fetch_msg_from_data_out_queue()

        # Default fallback to keep the connection alive
        return APDU.msg_success()

    def _try_fetch_msg_from_data_out_queue(self):
        """
        Check for data being ready for transmission to client

        :return: if true, data is available now for outgoing transfer
                 if false, no data is queued for transmission
        """
        # Buffer already filled with data and not fetched yet
        # We have to wait here, otherwise data is corrupted during an active read by the client
        if self.data_out_ready:
            return False

        try:
            if self.q_data_out.empty():
                return False
            # Try to get message
            # Calling still might raise an exception, because some incomplete data might be in the queue.
            msg_out = self.q_data_out.get_nowait()
            # Replace internal buffer
            self.data_out = msg_out
            self.data_out_offset = 0
            # Notify
            self.data_out_ready = True
            self.q_data_out.task_done()

            return True

        except queue.Empty:
            # Nothing to send, ignore this request
            self.log.debug('Empty queue exception while probing.')
            pass

        return False

    def _push_data_out_size(self, rx_buffer):
        if not self.data_out_ready:
            self.log.info('Memory locked, busy here.')
            return APDU.msg_err_no_data()

        #elif 0x02 == rx_buffer[APDU.LC]:
        # Reset push offset to give client a new chance to get the whole msg with fragmentation
        self.data_out_offset = 0
        # Request: size of available data
        data_size = len(self.data_out)
        # Answer: P1 * 256 + P2 encodes size
        response = struct.pack('!H2s', data_size, APDU.SUCCESS)  # Put as unsigned short with 2 bytes + success
        return response, 4

        #else:
        #    return APDU.msg_err_param()

    def _push_data_to_client(self, rx_buffer):
        """Read data_out bytearray and transmit fragmented packets to client"""
        if not self.data_out_ready:
            return APDU.msg_err_no_data()

        # Unpack starting from P1: [ .. | P1 | P2 | LC | .. ]
        #                                 ^offset^  ^requested_bytes
        offset, requested_bytes = struct.unpack_from('!HB', rx_buffer, APDU.P1)
        requested_bytes = min(requested_bytes, self.MAX_CHUNK_SIZE)

        # Requested data has to be within bounds of data_out buffer
        if (offset + requested_bytes) > len(self.data_out): # TODO: check for requested_bytes > 0
            return APDU.msg_err_param()

        # Set offset automatically for lazy client (might become default, then remove code with indexing)
        if offset == 0:
            offset = self.data_out_offset

        self.log.debug('Offset %d, available %d', offset, len(self.data_out))
        buf_out = bytearray(self.data_out[offset:offset+requested_bytes])
        buf_out.extend(APDU.SUCCESS)
        self.data_out_offset += requested_bytes

         # Notify about finished data transfer and free for new data if end of data is reached
        if (offset + requested_bytes) == len(self.data_out):
            self.log.debug('Pushed successfully %d bytes to client', len(self.data_out))
            self.data_out = bytearray()
            self.data_out_ready = False
            self.data_out_offset = 0

        return buf_out, len(buf_out)

    def _get_data_in_size(self, rx_buffer):
        """Announces the length for the incoming data"""
        if not self.data_in_ready:
            self.log.info('Memory locked, busy here.')
            return APDU.msg_err_not_ready()

        # Extract requested size, but upper bound to our limit
        requested_size = struct.unpack_from('!H', rx_buffer, APDU.P2)[0]
        requested_size = min(requested_size, self.MAX_BYTES)

        # Prepare buffer for merging data chunks and remove old buffer if not busy
        self.data_in = bytearray()  # BUG: could be misused to trigger a lot of small-sized memory allocations.
                                    # Assuming, GC is fast enough for cleaning up.
        self.data_in_size = requested_size
        self.log.debug('Allocating %d bytes for data input', requested_size)
        # Response: actually reserved bytes
        response = struct.pack('!H2s', requested_size, APDU.SUCCESS)

        return response, 4

    def _get_data_from_client(self, rx_buffer):
        """Collect received chunks into data_in and notifies a listener if the transfer is finished"""
        if not self.data_in_ready:
            return APDU.msg_err_not_ready()

        # Message size without header
        input_len = len(rx_buffer) - APDU.DATA
        # Offset starting from P1: [ .. | P1 | P2 | LC | .. ] (unsigned short: 2 bytes)
        #                                 ^offset^  ^ignore as given by input_len
        offset = struct.unpack_from('!H', rx_buffer, APDU.P1)[0]

        if (offset + input_len) > self.data_in_size or input_len > self.MAX_CHUNK_SIZE:
            return APDU.msg_err_param()

        if (len(self.data_in) + input_len) > self.data_in_size:
            # TODO: Clear buffer as received data might be only rubbish now
            return APDU.msg_err_not_enough_space()

        # Insert message chunk into buffer
        # - If no offset is defined, data is assembled automatically
        if offset == 0:
            self.data_in.extend(rx_buffer[APDU.DATA:])
        else:
            self.data_in[offset:offset+input_len] = rx_buffer[APDU.DATA:]

        # Notify about finished data transfer and deny external modification by client
        if len(self.data_in) == self.data_in_size:
            self.data_in_ready = False
            # Transfer to other thread
            self.q_data_in.put(self.data_in)
            self.data_in = bytearray()
            self.log.debug('data_in buffer (%d bytes) completed', self.data_in_size)
            self.data_in_ready = True
            # data_in is now complete for local processing. This tells the client that all data was received
            # successfully, but we need some time until we have data to push back to the client.
            return APDU.msg_success_with_data_ready(0x00)

        return APDU.msg_success()

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

    # APDU status messages
    SUCCESS                 = b'\x90\x00'
    ERR_PARAMS              = b'\x6a\x00'
    ERR_WRONG_DATA          = b'\x6a\x80'
    ERR_NO_DATA             = b'\x6a\x82'
    ERR_NOT_ENOUGH_SPACE    = b'\x6a\x84'
    # Custom status messages
    ERR_NOT_READY           = b'\x6a\x90'

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