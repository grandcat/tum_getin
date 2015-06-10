#ifndef NFC_DEVICE_H
#define NFC_DEVICE_H

#include <string>

#include <nfc/nfc.h>

namespace nfc_communicator {

class nfc_connector
{
public:
    nfc_connector();
    ~nfc_connector();

    void open_nfc_hardware();

    void send_message(std::string msg);
    std::string receive_message();

private:
    nfc_device* _pnd;
    nfc_context* _context;
    nfc_target nt;      ///< libnfc target
};

}

#endif // NFC_DEVICE_H
