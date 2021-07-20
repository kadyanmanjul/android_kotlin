package com.joshtalks.joshskills.ui.voip

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object VoipAudioState {
    private val currentState = MutableStateFlow<State>(State.EarpieceOn)
    val audioState: StateFlow<State> = currentState

    fun switchToHeadphone() {
        currentState.value = State.HeadphoneOn
    }

    fun switchToBluetooth() {
        currentState.value = State.BluetoothOn
    }

    fun switchToSpeaker() {
        currentState.value = State.SpeakerOn
    }

    fun switchToEarpiece() {
        currentState.value = State.EarpieceOn
    }
}

sealed class State {
    object EarpieceOn : State()
    object HeadphoneOn : State()
    object BluetoothOn : State()
    object SpeakerOn : State()
}