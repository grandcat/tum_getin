#include <zmqpp/zmqpp.hpp>
#include <string>
#include <iostream>
#include <chrono>
#include <thread>

using namespace std;

int main(int argc, char *argv[]) {
  const string endpoint = "tcp://*:5556"; // *:4242
  //const string endpoint = "ipc://nfc.ipc";

  // initialize the 0MQ context
  zmqpp::context context;

  // generate a pair socket
  zmqpp::socket_type type = zmqpp::socket_type::pair; // pair
  zmqpp::socket socket (context, type);

  // bind to the socket
  cout << "Binding to " << endpoint << "..." << endl;
  socket.bind(endpoint);

  while(true) {
  // send response (bi-directional messages)
  zmqpp::message send_msg;
  send_msg << "Hello World!x9832";
  cout << "Sending..." << endl;
  socket.send(send_msg);

  // receive the message
  cout << "Receiving message..." << endl;
  zmqpp::message message;
  // decompose the message 
  socket.receive(message);
  string text;
  message >> text;

  cout << "Received text:\"" << text << endl;

  std::this_thread::sleep_for(std::chrono::milliseconds(1000));
  }
}
