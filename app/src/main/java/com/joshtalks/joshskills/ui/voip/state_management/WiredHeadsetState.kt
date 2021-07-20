package com.joshtalks.joshskills.ui.voip.state_management

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class WiredHeadsetState {
    private val currentState = MutableStateFlow<WiredState>(WiredState.Disconnected)
    val wiredState: StateFlow<WiredState> = currentState

    fun wiredHeadsetConnected() {
        currentState.value = WiredState.Disconnected
    }

    fun wiredHeadsetDisconnected() {
        currentState.value = WiredState.Connected
    }
}

sealed class WiredState {
    object Connected : WiredState()
    object Disconnected : WiredState()
}