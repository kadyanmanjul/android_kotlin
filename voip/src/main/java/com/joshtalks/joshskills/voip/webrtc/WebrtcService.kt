package com.joshtalks.joshskills.voip.webrtc

import android.telecom.Call
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow

internal interface WebrtcService {
    suspend fun connectCall(request: CallRequest) // Need Arguments
    suspend fun disconnectCall() // Might Need Arguments
    fun muteAudioStream(muteAudio : Boolean)
    fun enableSpeaker(speaker :Boolean)
    fun observeCallingEvents() : SharedFlow<CallState> // Will return value
    fun onDestroy()
}

internal interface CallRequest {
    fun getChannel() : String
    fun getToken() : String
    fun getAgoraUId() : Int
}

internal sealed class CallState {
    object Idle : CallState()
    object OnHold : CallState()
    object OnUnHold : CallState()
    object OnMute : CallState()
    object OnUnMute : CallState()
    object OnReconnecting : CallState()
    object OnReconnected : CallState()
    object CallConnected : CallState() // Remote User Join The Channel
    object CallDisconnected : CallState()
    object ReconnectingFailed : CallState()
    object CallInitiated : CallState() // Local User Join The Channel
    object Error : CallState()
    object UserAlreadyDisconnectedError: CallState()
    object UserLeftChannel: CallState()
}

//internal enum class State {
//    IDLE, // Doing Nothing - Can make Call
//    JOINING, // Join Channel Called and Success Returned but haven't joined the channel
//    JOINED, // Local User Joined the Channel
//    CONNECTED, // Remote User Joined the Channel and can Talk
//    LEAVING // LeaveChannel Called but haven't left the channel
//}