#backendConnection.py

- The Class can be used to communicate with the Backend server
- The constructor of the Backend-class needs the host and the port (e.g b=Backend('localhost',3000))
- The class is offering the methods
  - isConnected(): checks if server is reachable by comparing the string answer when using a GET request to '/'
  - isStudent(pseudoID): checks if the specific pseudoID is a valid student
  - getPublicKey(pseudoID): gets the public key of the student with the pseudoID

