package com.joshtalks.joshskills.ui.call.lib

import kotlinx.coroutines.flow.Flow

interface CallingService {
    fun connectCall(request: CallRequest) // Need Arguments
    fun disconnectCall() // Might Need Arguments
    fun observeCallingEvents() : Flow<CallState>// Will return value
}

interface CallRequest {
    fun getChannel() : String
}

enum class CallState {
    IDLE, ON_HOLD, ON_RECONNECTING, CALL_CONNECTED, CALL_DISCONNECTED
}