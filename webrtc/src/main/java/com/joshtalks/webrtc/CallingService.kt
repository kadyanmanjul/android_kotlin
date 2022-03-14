package com.joshtalks.joshskills.ui.call.webrtc

import kotlinx.coroutines.flow.Flow

interface CallingService {
    suspend fun initCallingService()
    fun connectCall(request: CallRequest) // Need Arguments
    fun disconnectCall() // Might Need Arguments
    fun observeCallingEvents() : Flow<CallState> // Will return value
}

interface CallRequest {
    fun getChannel() : String
}

/*
enum class CallState {
    IDLE,
    HOLD,
    UNHOLD,
    MUTE,
    UNMUTE,
    RECONNECTING,
    RECONNECTED,
    INITIATED, // Local User Join The Channel
    CONNECTED, // Remote User Join The Channel
    DISCONNECTED,
}*/

sealed class CallState {
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
}