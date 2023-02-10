package com.joshtalks.joshskills.premium.calling.audiocontroller

import android.media.AudioManager
import kotlinx.coroutines.flow.SharedFlow

internal interface IAudioController {
    fun activate()
    fun switchAudioToSpeaker()
    fun switchAudioToDefault()
    fun deactivate()
}

interface IAudioRouteListener {
    fun registerAudioControllerReceivers()
    fun unregisterAudioControllerReceivers()
    fun getCurrentAudioRoute(): AudioRouteConstants
    fun changeSpeakerState(isSpeakerOn: Boolean)
    fun refresh(audioManager: AudioManager?)
    fun setRouteListener(function: () -> Unit)
}