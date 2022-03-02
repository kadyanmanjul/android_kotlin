package com.joshtalks.joshskills.ui.voip.calling_state

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class CallingState() {

    private val mutex = Mutex()

    companion object {
        var callerAgoraId = ""
            private set
        var callieAgoraId = ""
            private set
        var callAgoraId = ""
            private set
        var channelNameAgora = ""
            private set
        var callerName = ""
            private set
        var callerImageLink = ""
            private set
    }

    suspend fun reset() {
        mutex.withLock {
            callerAgoraId = ""
            callieAgoraId = ""
            callAgoraId = ""
            channelNameAgora = ""
            callerName = ""
            callerImageLink = ""

        }
    }
}