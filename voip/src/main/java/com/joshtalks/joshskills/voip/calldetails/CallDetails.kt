package com.joshtalks.joshskills.voip.calldetails

import com.joshtalks.joshskills.voip.communication.model.ChannelData
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
    var agoraChannelName = ""
        private set
    var remoteUserName = ""
        private set
    var remoteUserImageUrl : String? = null
        private set

    suspend fun reset() {
        mutex.withLock {
            remoteUserAgoraId = -1
            localUserAgoraId = -1
            callId = -1
            agoraChannelName = ""
            remoteUserName = ""
            remoteUserImageUrl = null

        }
    }

    suspend fun set(details : ChannelData) {
        mutex.withLock {
            remoteUserAgoraId = details.getPartnerUid()
            localUserAgoraId = details.getAgoraUid()
            callId = details.getCallingId()
            agoraChannelName = details.getChannel()
            remoteUserName = details.getCallingPartnerName()
            remoteUserImageUrl = details.getCallingPartnerImage()
        }
    }
}