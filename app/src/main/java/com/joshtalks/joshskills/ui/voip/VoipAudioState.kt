package com.joshtalks.joshskills.ui.voip

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object VoipAudioState {
    private val currentState = MutableStateFlow<State>(State.Default(false))
    val audioState: StateFlow<State> = currentState

    fun switchToDefault(isWiredHeadphonePluggedIn: Boolean) {
        currentState.value = State.Default(isWiredHeadphonePluggedIn)
    }

    fun switchToBluetooth() {
        currentState.value = State.BluetoothOn
    }

    fun switchToSpeaker() {
        currentState.value = State.SpeakerOn
    }
}

sealed class State {
    data class Default(val isWiredHeadphonePluggedIn: Boolean) : State()
    object BluetoothOn : State()
    object SpeakerOn : State()
}