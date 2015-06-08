#ifndef APDUMESSAGE_H
#define APDUMESSAGE_H

#include <vector>

class APDUMessage
{
public:
    enum ISO7816_ActionType
    {
        APDUMsg_TAG,
        APDUmsg_READ_BINARY     = 0xb0,
        APDUmsg_UPDATE_BINARY   = 0xd6,
    };

private:
    ISO7816_ActionType m_type;
    std::vector<char> m_raw;


public:
    APDUMessage();

    void setActionType(ISO7816_ActionType type);
};

#endif // APDUMESSAGE_H
