package com.joshtalks.joshskills.voip.communication.model

sealed interface Communication {
    fun getType() : Int
    fun getEventTime() : Long?
}

class Error(val reason : String) : Communication {
    override fun getType(): Int {
        return -1
    }

    override fun getEventTime(): Long? {
       return null
    }
}

sealed interface IncomingData : Communication {
    fun getChannel() : String
    fun getMsgData() : String{
        return ""
    }
}

sealed interface OutgoingData : Communication {
    override fun getEventTime(): Long? { return null }
    fun getAddress(): String?
}

sealed interface InterestData : IncomingData {
    fun getInterestHeader() : String
    fun getInterests() : List<String>
}

sealed interface UIState : UserActionData {
    fun isHold() : Boolean
    fun isMute() : Boolean
}

sealed interface IncomingCallData : Communication {
    fun getCallId() : Int
}

interface NetworkActionData : OutgoingData {
    fun getUserId() : Int
    fun getDuration() : Long
    fun getChannelName() : String
}

interface UserActionData : OutgoingData {
    fun getChannelName() : String
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
    fun getPartnerMentorId() : String
    fun getTopicImage() : String
    fun getOccupation() : String
    fun getAspiration() : String
    fun isNewSearchingEnabled() : Boolean
}

class IncorrectCommunicationDataException(message: String) : Exception(message)

