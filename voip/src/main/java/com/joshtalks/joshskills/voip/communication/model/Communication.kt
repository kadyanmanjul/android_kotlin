package com.joshtalks.joshskills.voip.communication.model

sealed interface Communication {
    fun getType() : Int
    fun getEventTime() : Long?
}

class Error(val errorType : Int = -1) : Communication {
    override fun getType(): Int {
        return errorType
    }

    override fun getEventTime(): Long? {
       return null
    }
}

sealed interface IncomingData : Communication {
    fun getChannel() : String
}

sealed interface OutgoingData : Communication {
    override fun getEventTime(): Long? { return null }
}

sealed interface IncomingCallData : Communication {
    fun getCallId() : Int
}

interface NetworkActionData : OutgoingData {
    fun getCallingId() : Int
    fun getUserId() : Int
    fun getDuration() : Long
}

interface UserActionData : OutgoingData {
    fun getCallingId() : Int
}

interface MessageData : IncomingData

interface ChannelData : IncomingData {
    fun getCallingPartnerName() : String
    fun getCallingPartnerImage() : String?
    fun getCallingTopic() : String
    fun getCallingId() : Int
    fun getCallingToken() : String
    fun getAgoraUid() : Int
    fun getPartnerUid() : Int
}

class IncorrectCommunicationDataException(message: String) : Exception(message)

