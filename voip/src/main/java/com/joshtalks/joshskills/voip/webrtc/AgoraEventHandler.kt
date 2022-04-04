package com.joshtalks.joshskills.voip.webrtc

import com.joshtalks.joshskills.voip.voipLog
import io.agora.rtc.Constants
import io.agora.rtc.IRtcEngineEventHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch

const val USER_DROP_OFFLINE = 1
const val RECONNECTING_TIMEOUT_IN_MILLIS = 20 * 1000L
internal class AgoraEventHandler private constructor() : IRtcEngineEventHandler() {

    companion object {
        @Volatile private lateinit var INSTANCE: AgoraEventHandler
        @Volatile private lateinit var scope : CoroutineScope
        private var reconnectingJob : Job? = null
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
        scope.launch {
            if(reason == USER_DROP_OFFLINE)
                emitEvent(CallState.ReconnectingFailed)
        }
    }

    // Occurs when a user rejoins the channel after being disconnected due to network problems
    override fun onRejoinChannelSuccess(channel: String?, uid: Int, elapsed: Int) {
        voipLog?.log("Channel -> $channel and UID -> $uid")
        scope.launch {
            stopReconnectingTimeoutTimer()
            emitEvent(CallState.OnReconnected)
        }
    }

    // Occurs when the SDK cannot reconnect to Agora server after its connection to the server is interrupted.
    override fun onConnectionLost() {
        voipLog?.log("Connection Lost")
        // The SDK triggers this callback when it cannot connect to the server 10 seconds after calling joinChannel(), regardless of whether it is in the channel or not.
//        scope.launch {
//            emitEvent(CallState.CallDisconnect)
//        }
    }

    // Occurs when the network connection state changes like RECONNECTING
    override fun onConnectionStateChanged(state: Int, reason: Int) {
        scope.launch {
            voipLog?.log("State -> $state and Reason -> $reason")
            if (Constants.CONNECTION_STATE_RECONNECTING == state &&
                reason == Constants.CONNECTION_CHANGED_INTERRUPTED) {
                emitEvent(CallState.OnReconnecting)
                startReconnectingTimeoutTimer()
            }
        }
    }

    private fun emitEvent(event : CallState) {
        voipLog?.log("Event -> $event")
        scope.launch {
            callingEvent.emit(event)
        }
    }

    private fun startReconnectingTimeoutTimer() {
        reconnectingJob = scope.launch {
            delay(RECONNECTING_TIMEOUT_IN_MILLIS)
            emitEvent(CallState.ReconnectingFailed)
        }
    }

    private fun stopReconnectingTimeoutTimer() {
            reconnectingJob?.cancel()
    }
}

// TODO: Need to change Class name
internal data class AgoraEvent(val handler : AgoraEventHandler?, val callingEvent : SharedFlow<CallState>)