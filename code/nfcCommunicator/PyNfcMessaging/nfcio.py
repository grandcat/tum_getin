import logging
import queue
import time


class NfcIO(object):
    """
    :type q_data_in: queue
    :type q_data_out: queue
    """

    def __init__(self, queue_data_in, queue_data_out,log=None):
        self.log = log or logging.getLogger(__name__)
        self.q_data_in = queue_data_in
        self.q_data_out = queue_data_out


    def register_callback(self, fn):
        pass

    def run_ioloop(self):
        msg_loop = True

        while msg_loop:
            # Wait for a complete incoming message
            # Typically, it is a bytearray containing
            msg_in = self.q_data_in.get()
            self.log.info('NfcIO got something, but I pretend to do some work now :p')
            # time.sleep(1)
            self.log.info('NfcIO received from reader: %s', msg_in.decode('utf-8'))
            self.q_data_in.task_done()

            # TODO: exchange events (e.g., new initiator detected) between nfcio and nfc_reader

            # Send something
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
            self.q_data_out.put(msg_out)

            # break
        return msg_loop

    def connection_made(self):
        pass

    def data_received(self, msg):
        pass