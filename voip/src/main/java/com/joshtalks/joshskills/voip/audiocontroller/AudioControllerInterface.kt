package com.joshtalks.joshskills.voip.audiocontroller

import kotlinx.coroutines.flow.SharedFlow

internal interface AudioControllerInterface {
    fun registerAudioControllerReceivers()
    fun observeAudioRoute(): SharedFlow<AudioRouteConstants>
    fun switchAudioToSpeaker()
    fun switchAudioToDefault()
}