package com.joshtalks.joshskills.voip.webrtc

import kotlinx.coroutines.flow.Flow

internal interface CallingService {
    suspend fun initCallingService()
    fun connectCall(request: CallRequest) // Need Arguments
    fun disconnectCall() // Might Need Arguments
    fun observeCallingEvents() : Flow<CallState> // Will return value
    fun observeCallingState() : Flow<State> // Will Return State
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
    object CallInitiated : CallState() // Local User Join The Channel
    object Error : CallState()
}

internal enum class State {
    IDLE, // Doing Nothing - Can make Call
    JOINING, // Join Channel Called and Success Returned but haven't joined the channel
    JOINED, // Local User Joined the Channel
    CONNECTED, // Remote User Joined the Channel and can Talk
    LEAVING // LeaveChannel Called but haven't left the channel
}