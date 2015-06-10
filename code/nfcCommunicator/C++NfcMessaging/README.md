Introduction
============
The nfcCommunicator is responsible for interfacing with the NFC transceiver
hardware and handling long data frames by a custom fragmentation.

For sending and receiving data frames, this module exposes an ZeroMQ based
interface This allows to connect an application to the reader independent of the
utilized programming language (e.g., Python in our case).

ToDO
----
- Fragmentation
- APDU abstraction (part of this module)
- Card emulation within application (--> not using nfc-emulation.h) for proper
  error handling
- ZeroMQ interface (tested simple C++ <-> Python string exchange so far.)
