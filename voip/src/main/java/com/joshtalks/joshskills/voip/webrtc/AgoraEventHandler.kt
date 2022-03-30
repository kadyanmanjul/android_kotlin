package com.joshtalks.joshskills.voip.webrtc

import com.joshtalks.joshskills.voip.voipLog
import io.agora.rtc.IRtcEngineEventHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex

internal class AgoraEventHandler private constructor() : IRtcEngineEventHandler() {

    companion object {
        @Volatile private lateinit var INSTANCE: AgoraEventHandler
        @Volatile private lateinit var scope : CoroutineScope

        private val callingEvent by lazy<MutableSharedFlow<CallState>> {
            MutableSharedFlow(replay = 0)
        }

        fun getAgoraEventObject(scope: CoroutineScope) : AgoraEvent {
            voipLog?.log("Creating Agora Handler")
            if (this::INSTANCE.isInitialized)
                return AgoraEvent(INSTANCE, callingEvent)
            else
                synchronized(this) {
                    return if (this::INSTANCE.isInitialized)
                        AgoraEvent(INSTANCE, callingEvent)
                    else {
                        Companion.scope = scope
                        AgoraEvent(AgoraEventHandler().also { INSTANCE = it }, callingEvent)
                    }
                }
            voipLog?.log("Agora Handler Object --> $INSTANCE")
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
    ) {
        voipLog?.log("$totalVolume")
    }

    // Occurs when the local audio playback route changes
    override fun onAudioRouteChanged(routing: Int) {
        voipLog?.log("$routing")
    }

    // Reports an error during SDK runtime
    override fun onError(errorCode: Int) {
        voipLog?.log("$errorCode")
        emitEvent(CallState.Error)
    }

    // Occurs when the local user joins a specified channel (#joinChannel)
    override fun onJoinChannelSuccess(channel: String, uid: Int, elapsed: Int) {
        voipLog?.log("Joined Channel -> $channel and UID -> $uid ")
        emitEvent(CallState.CallInitiated)
    }

    // Occurs when a remote user (COMMUNICATION)/host (LIVE_BROADCASTING) joins the channel.
    override fun onUserJoined(uid: Int, elapsed: Int) {
        voipLog?.log("UID -> $uid")
        emitEvent(CallState.CallConnected)
    }

    // Occurs when a user leaves the channel(#leaveChannel)
    override fun onLeaveChannel(stats: RtcStats) {
        voipLog?.log("$stats")
        emitEvent(CallState.CallDisconnected)
    }


    // Occurs when a remote user (COMMUNICATION)/host (LIVE_BROADCASTING) leaves the channel.
    // 1. Leave the channel
    // 2. Drop offline
    override fun onUserOffline(uid: Int, reason: Int) {
        voipLog?.log("UID -> $uid and Reason -> $reason")
    }

    // Occurs when a user rejoins the channel after being disconnected due to network problems
    override fun onRejoinChannelSuccess(channel: String?, uid: Int, elapsed: Int) {
        voipLog?.log("Channel -> $channel and UID -> $uid")
    }

    // Occurs when the SDK cannot reconnect to Agora server after its connection to the server is interrupted.
    override fun onConnectionLost() {
        voipLog?.log("Connection Lost")
    }

    // Occurs when the network connection state changes like RECONNECTING
    override fun onConnectionStateChanged(state: Int, reason: Int) {
        voipLog?.log("State -> $state and Reason -> $reason")
    }

    private fun emitEvent(event : CallState) {
        voipLog?.log("Event -> $event")
        scope.launch {
            callingEvent.emit(event)
        }
    }
}

// TODO: Need to change Class name
internal data class AgoraEvent(val handler : AgoraEventHandler?, val callingEvent : SharedFlow<CallState>)