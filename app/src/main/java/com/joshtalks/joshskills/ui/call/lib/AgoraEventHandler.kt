package com.joshtalks.joshskills.ui.call.lib

import android.telecom.Call
import io.agora.rtc.IRtcEngineEventHandler
import java.util.PriorityQueue
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
// #1
class AgoraEventHandler private constructor() : IRtcEngineEventHandler() {
    val mutex = Mutex()

    companion object {
        @Volatile private lateinit var INSTANCE: AgoraEventHandler
        @Volatile private lateinit var scope : CoroutineScope

        private val callingEvent by lazy<MutableStateFlow<CallState>> {
            MutableStateFlow(CallState.Idle)
        }

        fun getAgoraEventObject(scope: CoroutineScope) : AgoraEvent {
            if (this::INSTANCE.isInitialized)
                return AgoraEvent(INSTANCE, callingEvent)
            else
                synchronized(this) {
                    return if (this::INSTANCE.isInitialized)
                        AgoraEvent(INSTANCE, callingEvent)
                    else {
                        this.scope = scope
                        AgoraEvent(AgoraEventHandler().also { INSTANCE = it }, callingEvent)
                    }
                }
        }
    }

    /**
     * 1. 1st ----> Join
     * +++++++++++++++++++++++
     * 2. Last ---> LeaveChannel ---- Clear all pending code execution
     */

    // Reports the volume information of users
    override fun onAudioVolumeIndication(
        speakers: Array<out IRtcEngineEventHandler.AudioVolumeInfo>?,
        totalVolume: Int
    ) {}

    // Occurs when the local audio playback route changes
    override fun onAudioRouteChanged(routing: Int) {}

    // Reports an error during SDK runtime
    override fun onError(errorCode: Int) {}

    // Occurs when the local user joins a specified channel (#joinChannel)
    override fun onJoinChannelSuccess(channel: String, uid: Int, elapsed: Int) {
        emitEvent(CallState.CallInitiated)
    }

    // Occurs when a remote user (COMMUNICATION)/host (LIVE_BROADCASTING) joins the channel.
    override fun onUserJoined(uid: Int, elapsed: Int) {
        emitEvent(CallState.CallConnected)
    }

    // Occurs when a user leaves the channel(#leaveChannel)
    override fun onLeaveChannel(stats: RtcStats) {
        emitEvent(CallState.CallDisconnected)
    }


    // Occurs when a remote user (COMMUNICATION)/host (LIVE_BROADCASTING) leaves the channel.
    // 1. Leave the channel
    // 2. Drop offline
    override fun onUserOffline(uid: Int, reason: Int) {}

    // Occurs when a user rejoins the channel after being disconnected due to network problems
    override fun onRejoinChannelSuccess(channel: String?, uid: Int, elapsed: Int) {}

    // Occurs when the SDK cannot reconnect to Agora server after its connection to the server is interrupted.
    override fun onConnectionLost() {}

    // Occurs when the network connection state changes like RECONNECTING
    override fun onConnectionStateChanged(state: Int, reason: Int) {}

    private fun emitEvent(event : CallState) {
        scope.launch {
            callingEvent.emit(event)
        }
    }

}

// TODO: Need to change Class name
data class AgoraEvent(val handler : AgoraEventHandler?, val callingEvent : SharedFlow<CallState>)