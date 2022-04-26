package com.joshtalks.joshskills.voip.calldetails

import android.util.Log
import com.joshtalks.joshskills.voip.communication.model.ChannelData
import com.joshtalks.joshskills.voip.mediator.CallDirection
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private const val TAG = "CallDetails"
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
    var partnerMentorId : String? = null
        private set

    suspend fun reset() {
        mutex.withLock {
            Log.d(TAG, "reset: $this")
            remoteUserAgoraId = -1
            localUserAgoraId = -1
            callId = -1
            callType = -1
            agoraChannelName = ""
            remoteUserName = ""
            remoteUserImageUrl = null
            topicName = ""
            partnerMentorId = ""
            Log.d(TAG, "reset: $this")
        }
    }

    suspend fun set(details : ChannelData, callType : Int) {
        mutex.withLock {
            Log.d(TAG, "set: Previous -> $this")
            remoteUserAgoraId = details.getPartnerUid()
            localUserAgoraId = details.getAgoraUid()
            callId = details.getCallingId()
            this.callType = callType
            agoraChannelName = details.getChannel()
            remoteUserName = details.getCallingPartnerName()
            remoteUserImageUrl = details.getCallingPartnerImage()
            topicName = details.getCallingTopic()
            partnerMentorId = details.getPartnerMentorId()
            Log.d(TAG, "set: Setting -> $details")
            Log.d(TAG, "set: After -> $this")
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
            Log.d(TAG, "reset: $")
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