package com.joshtalks.joshskills.voip.audiocontroller

import kotlinx.coroutines.flow.MutableSharedFlow

interface AudioControllerInterface {
    fun checkIfSpeakerOn():Boolean
    fun checkIfHeadsetPluggedIn():Boolean
    fun checkIfBluetoothConnect():Boolean
    fun registerAudioControllerReceivers()
    fun observeAudioRoute(): MutableSharedFlow<AudioRouteConstants>
    fun switchAudioToSpeaker()
    fun switchAudioToEarpiece()
}