import zmq
import random
import sys
import time

port = "5556"
context = zmq.Context()
socket = context.socket(zmq.PAIR)
socket.connect("tcp://localhost:%s" % port)
# socket.connect("ipc://nfc.ipc")

while True:
    msg = socket.recv()
    print(msg)
    socket.send_string('Client message to server 2')
    socket.send_string('Client message to server 2')
    time.sleep(1)
