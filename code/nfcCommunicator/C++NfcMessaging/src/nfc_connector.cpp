#include <cstring>
#include <stdexcept>

#include "nfc-utils.h"

#include "nfc_connector.h"

namespace nfc_communicator {

static constexpr const u_int8_t DEFAULT_ABTUID[] = {0x08, 0x00, 0xb0, 0xfe};
static constexpr const u_int8_t DEFAULT_ABTATS[] = {0x75, 0x33, 0x92, 0x03};

/**
 * \brief nfc_device constructor
 */
nfc_connector::nfc_connector()
{
    // Initialize libnfc structures for interfacing with NFC hardware
    // Modulation
    nt.nm.nmt = NMT_ISO14443A;
    nt.nm.nbr = NBR_UNDEFINED;
    // Card specific
    nfc_iso14443a_info& emulated_card = nt.nti.nai;
    emulated_card.abtAtqa[0] = 0x00;
    emulated_card.abtAtqa[1] = 0x04;
    std::memcpy(emulated_card.abtUid,   // Serial number:
                DEFAULT_ABTUID,         // - Hard-wired to 4 bytes on PN53x
                sizeof DEFAULT_ABTUID); // - First byte 0x08 for random UID
    emulated_card.szUidLen = 4;
    emulated_card.btSak = 0x20;
    std::memcpy(emulated_card.abtAts,   // Not used by PN532
                DEFAULT_ABTATS,
                sizeof DEFAULT_ABTATS);
    emulated_card.szAtsLen = 4;
}

/**
 * \brief nfc_device destructor
 */
nfc_connector::~nfc_connector()
{
    if (_pnd != nullptr)
        nfc_close(_pnd);
    if (_context != nullptr)
        nfc_exit(_context);
}

void nfc_connector::open_nfc_hardware()
{
    nfc_init(&_context);
    if (_context == nullptr) {
        throw std::runtime_error("nfc_init: unable to init libnfc (malloc).");
    }

    // Try to open the NFC reader
    _pnd = nfc_open(_context, 0);
    if (_pnd == nullptr) {
        throw std::runtime_error("nfc_open: unable to open NFC device.");
    }
}

std::string nfc_connector::receive_message()
{

}

}

