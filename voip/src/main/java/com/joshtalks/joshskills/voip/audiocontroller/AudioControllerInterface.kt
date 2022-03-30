package com.joshtalks.joshskills.voip.audiocontroller

import kotlinx.coroutines.flow.MutableSharedFlow

internal interface AudioControllerInterface {
    fun registerAudioControllerReceivers()
    fun observeAudioRoute(): MutableSharedFlow<AudioRouteConstants>
    fun switchAudioToSpeaker()
    fun switchAudioToEarpiece()
}