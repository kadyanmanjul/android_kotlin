package com.joshtalks.joshskills.voip.audiocontroller

import android.bluetooth.BluetoothHeadset
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import com.joshtalks.joshskills.voip.Utils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

/**
 * Require Permissions :-
 * 1.Bluetooth
 * 2.Bluetooth Connect
 * 3.Modify Audio Settings
 *
 * Require Receivers @BluetoothReceiver @HeadsetReceiver with these filter :-
 * 1.ACTION_CONNECTION_STATE_CHANGED
 * 2.HEADSET_PLUG
 *
 * Require to Call Method @registerAudioControllerReceivers() to explicitly registering above receivers
 */

class AudioController : AudioControllerInterface {

    private val applicationContext=Utils.context
    private val audioManager = applicationContext?.getSystemService(Context.AUDIO_SERVICE) as AudioManager?
    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    init {
        autoRouteSwitcher()
    }

    override fun registerAudioControllerReceivers() {
        val headsetFilter = IntentFilter().apply {
            addAction(Intent.ACTION_HEADSET_PLUG)
        }
        val bluetoothFilter = IntentFilter().apply {
            addAction(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED)
        }
        applicationContext?.registerReceiver(HeadsetReceiver(), headsetFilter)
        applicationContext?.registerReceiver(BluetoothReceiver(), bluetoothFilter)
    }

    override fun observeAudioRoute(): MutableSharedFlow<AudioRouteConstants> {
        return audioRouteFlow
    }

    override fun switchAudioToSpeaker() {
        audioManager?.mode = AudioManager.MODE_NORMAL
        audioManager?.isSpeakerphoneOn = true
    }

    override fun switchAudioToDefault() {
        audioManager?.mode = AudioManager.MODE_IN_COMMUNICATION
        audioManager?.isSpeakerphoneOn = false
    }

    private fun checkIfSpeakerOn(): Boolean {
        return audioManager?.isSpeakerphoneOn ?: false
    }

    private fun checkIfHeadsetPluggedIn(): Boolean {
        return audioManager?.isWiredHeadsetOn ?: false
    }

    private fun checkIfBluetoothConnect(): Boolean {
        return audioManager?.isBluetoothScoOn ?: false
    }

    internal fun checkAudioRoute() {
        if (checkIfSpeakerOn()) {
            emitAudioRoute(AudioRouteConstants.SpeakerAudio)
            return
        }
        if (checkIfBluetoothConnect()) {
            emitAudioRoute(AudioRouteConstants.BluetoothAudio)
            return
        }
        if (checkIfHeadsetPluggedIn()) {
            emitAudioRoute(AudioRouteConstants.HeadsetAudio)
            return
        }
        emitAudioRoute(AudioRouteConstants.EarpieceAudio)

    }

    private fun emitAudioRoute(audioRoute: AudioRouteConstants) {
        coroutineScope.launch {
            audioRouteFlow.emit(audioRoute)
        }
    }

    private fun autoRouteSwitcher() {
        coroutineScope.launch {
            audioRouteFlow.collect {
                if (AudioController().checkIfSpeakerOn()) {
                    AudioController().switchAudioToSpeaker()
                }
            }
        }
    }
}