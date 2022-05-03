package com.joshtalks.joshskills.voip.calldetails

import android.util.Log
import com.joshtalks.joshskills.voip.communication.model.ChannelData
import com.joshtalks.joshskills.voip.mediator.CallDirection
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Duration

private const val TAG = "CallDetails"

data class LastCallDetail(
    val remoteUserAgoraId : Int,
    val localUserAgoraId : Int,
    val callId : Int,
    val callType : Int,
    val agoraChannelName : String,
    val remoteUserName : String,
    val remoteUserImageUrl : String?,
    val topicName : String,
    val callDirection : CallDirection,
    val remoteUserMentorId : String,
    val durationInMilli : Long
    )

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