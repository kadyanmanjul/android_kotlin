package com.joshtalks.joshskills.ui.call.lib

import kotlinx.coroutines.flow.Flow

interface CallingService {
    fun connectCall(request: CallRequest) // Need Arguments
    fun disconnectCall() // Might Need Arguments
    fun observeCallingEvents() : Flow<CallState> // Will return value
}

interface CallRequest {
    fun getChannel() : String
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