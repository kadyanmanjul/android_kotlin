package com.joshtalks.joshskills.voip.webrtc

import kotlinx.coroutines.flow.Flow

interface CallingService {
    suspend fun initCallingService()
    fun connectCall(request: CallRequest) // Need Arguments
    fun disconnectCall() // Might Need Arguments
    fun observeCallingEvents() : Flow<CallState> // Will return value
}

interface CallRequest {
    fun getChannel() : String
    fun getToken() : String
}

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