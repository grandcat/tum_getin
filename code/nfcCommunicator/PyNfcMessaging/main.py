import logging

from nfcio import ReaderIO
from protocol import NFCReader

if __name__ == '__main__':
    logging.basicConfig(format='[%(levelname)s:%(threadName)s:%(name)s:%(funcName)s] %(message)s', level=logging.DEBUG)
    # Do not pass logger object any more to generate uniqu  e names per module
    logger = logging.getLogger(__name__)

    device = NFCReader()
    reader = ReaderIO(device)
    reader.run()
