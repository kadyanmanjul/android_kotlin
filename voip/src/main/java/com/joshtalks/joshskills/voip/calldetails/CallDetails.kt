package com.joshtalks.joshskills.voip.calldetails

import com.joshtalks.joshskills.voip.communication.model.ChannelData
import com.joshtalks.joshskills.voip.mediator.CallDirection
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

object CallDetails {
    private val mutex = Mutex()

    var remoteUserAgoraId : Int = -1
        private set
    var localUserAgoraId : Int = -1
        private set
    var callId : Int = -1
        private set
    var callType : Int = -1
        private set
    var agoraChannelName = ""
        private set
    var remoteUserName = ""
        private set
    var remoteUserImageUrl : String? = null
        private set
    var topicName = ""
        private set
    var callDirection = -1
        private set

    suspend fun reset() {
        mutex.withLock {
            remoteUserAgoraId = -1
            localUserAgoraId = -1
            callId = -1
            callType = -1
            agoraChannelName = ""
            remoteUserName = ""
            remoteUserImageUrl = null
            topicName = ""
        }
    }

    suspend fun set(details : ChannelData, callType : Int) {
        mutex.withLock {
            remoteUserAgoraId = details.getPartnerUid()
            localUserAgoraId = details.getAgoraUid()
            callId = details.getCallingId()
            this.callType = callType
            agoraChannelName = details.getChannel()
            remoteUserName = details.getCallingPartnerName()
            remoteUserImageUrl = details.getCallingPartnerImage()
            topicName = details.getCallingTopic()
        }
    }
}

object IncomingCallData {
    private val mutex = Mutex()

    var callId : Int = -1
        private set
    var callType : Int = -1
        private set

    suspend fun reset() {
        mutex.withLock {
            callId = -1
            callType = -1
        }
    }

    suspend fun set(callId : Int, callType : Int) {
        mutex.withLock {
            this.callId = callId
            this.callType = callType
        }
    }
}