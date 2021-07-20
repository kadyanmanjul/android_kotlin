package com.joshtalks.joshskills.ui.voip

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object VoipAudioState {
    private val currentState = MutableStateFlow<State>(State.Default(true))
    val audioState: StateFlow<State> = currentState

    fun switchToDefault(isHeadset: Boolean) {
        currentState.value = State.Default(isHeadset)
    }

    fun switchToBluetooth() {
        currentState.value = State.BluetoothOn
    }

    fun switchToSpeaker() {
        currentState.value = State.SpeakerOn
    }
}

sealed class State {
    data class Default(val isHeadset: Boolean) : State()
    object BluetoothOn : State()
    object SpeakerOn : State()
}