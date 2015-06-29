Deprecated
==========
Please note that the development of a C++ module was stopped early in favor of
a Python based version.
Other parts of the NFC reader are intended to be written in Python. For
compatibility reasons and being able to maintain an homogeneous code base, this
module will be in Python, too.

Introduction
============
The nfcCommunicator is responsible for interfacing with the NFC transceiver
hardware and handling long data frames by a custom fragmentation.

For sending and receiving data frames, this module exposes an ZeroMQ based
interface This allows to connect an application to the reader independent of the
utilized programming language.

ToDO
----
- Fragmentation
- APDU abstraction (part of this module)
- Card emulation within application (--> not using nfc-emulation.h) for proper
  error handling
- ZeroMQ interface (tested simple C++ <-> Python string exchange so far.)
