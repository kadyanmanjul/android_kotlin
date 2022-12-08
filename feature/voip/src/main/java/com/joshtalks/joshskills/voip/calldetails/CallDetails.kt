package com.joshtalks.joshskills.voip.calldetails

import android.util.Log
import com.joshtalks.joshskills.voip.constant.Category
import com.joshtalks.joshskills.voip.mediator.CallDirection
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

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
    var callType : Category = Category.PEER_TO_PEER
        private set

    suspend fun reset() {
        mutex.withLock {
            Log.d(TAG, "reset: $")
            callId = -1
            callType = Category.PEER_TO_PEER
        }
    }

    suspend fun set(callId : Int, callType : Category) {
        mutex.withLock {
            this.callId = callId
            this.callType = callType
        }
    }
}