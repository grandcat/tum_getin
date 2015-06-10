#include "apdumessage.h"

APDUMessage::APDUMessage()
{

}

void APDUMessage::setActionType(APDUMessage::ISO7816_ActionType type)
{
    this->m_type = type;
}

