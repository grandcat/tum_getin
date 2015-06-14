import zmq
import random
import sys
import time

port = "5556"
context = zmq.Context()
socket = context.socket(zmq.PAIR)
socket.bind("tcp://*:%s" % port)

while True:
    socket.send_string('Server message to client3')
    # Not waiting here, just probing, if we have time to
    time.sleep(1)
    try:
        msg = socket.recv_string(flags=zmq.NOBLOCK)
        print(msg)

    except zmq.ZMQError as e:
        if e.errno == zmq.EAGAIN:
            print("eagain: trying again.")
            # continue
        else:
            raise e


    time.sleep(1)
