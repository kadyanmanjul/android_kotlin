package com.joshtalks.joshskills.voip.communication.model

sealed interface Communication {
    fun getType() : Int
}

class Error(val errorType : Int = -1) : Communication {
    override fun getType(): Int {
        return errorType
    }
}

sealed interface IncomingData : Communication {
    fun getChannel() : String
}

sealed interface OutgoingData : Communication

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

interface MessageData : IncomingData {
    fun getMessage() : String
}

interface ChannelData : IncomingData {
    fun getCallingPartnerName() : String
    fun getCallingPartnerImage() : String?
    fun getCallingTopic() : String
    fun getCallingId() : Int
    fun getCallingToken() : String
}

class IncorrectCommunicationDataException(message: String) : Exception(message)

