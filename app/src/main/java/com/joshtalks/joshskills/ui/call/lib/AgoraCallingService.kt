package com.joshtalks.joshskills.ui.call.lib

import com.joshtalks.joshskills.BuildConfig
import com.joshtalks.joshskills.core.AppObjectController
import io.agora.rtc.IRtcEngineEventHandler
import io.agora.rtc.RtcEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class AgoraCallingService : CallingService {
    private val callingEvent : Flow<CallState> = MutableStateFlow(CallState.IDLE)
    private val agoraEngine by lazy<RtcEngine> {
        RtcEngine.create(
            AppObjectController.joshApplication,
            BuildConfig.AGORA_API_KEY,
            CallServiceHandler)
    }

    override fun connectCall(request: CallRequest) {

    }


    override fun disconnectCall() {
        TODO("Not yet implemented")
    }

    override fun observeCallingEvents(): Flow<CallState> {

        return callingEvent
    }
}

object CallServiceHandler : IRtcEngineEventHandler() {

    // Reports the volume information of users
    override fun onAudioVolumeIndication(
        speakers: Array<out IRtcEngineEventHandler.AudioVolumeInfo>?,
        totalVolume: Int
    ) {}

    // Occurs when the local audio playback route changes
    override fun onAudioRouteChanged(routing: Int) {}

    // Reports an error during SDK runtime
    override fun onError(errorCode: Int) {

    }

    // Occurs when the local user joins a specified channel
    override fun onJoinChannelSuccess(channel: String, uid: Int, elapsed: Int) {
        // User Joined
    }

    // Occurs when a user leaves the channel
    override fun onLeaveChannel(stats: RtcStats) {
        // User Leave's Channel
    }

    // Occurs when a remote user (COMMUNICATION)/host (LIVE_BROADCASTING) joins the channel.
    override fun onUserJoined(uid: Int, elapsed: Int) {
        // Opposite User Joined
    }

    // Occurs when a remote user (COMMUNICATION)/host (LIVE_BROADCASTING) leaves the channel.
    // 1. Leave the channel
    // 2. Drop offline
    override fun onUserOffline(uid: Int, reason: Int) {
        // User Join
    }

    // Occurs when a user rejoins the channel after being disconnected due to network problems
    override fun onRejoinChannelSuccess(channel: String?, uid: Int, elapsed: Int) {
        // User ReJoin The Channel
    }

    // Occurs when the SDK cannot reconnect to Agora server after its connection to the server is interrupted.
    override fun onConnectionLost() {

    }

    // Occurs when the network connection state changes like RECONNECTING
    override fun onConnectionStateChanged(state: Int, reason: Int) {

    }
}