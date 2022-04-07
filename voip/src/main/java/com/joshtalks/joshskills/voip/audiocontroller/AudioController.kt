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
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
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

class AudioController(val coroutineScope : CoroutineScope) : AudioControllerInterface {

    private val applicationContext=Utils.context
    private val audioManager = applicationContext?.getSystemService(Context.AUDIO_SERVICE) as AudioManager?
    private val audioRouteFlow = MutableSharedFlow<AudioRouteConstants>()
    private val headsetReceiver by lazy {
        HeadsetReceiver(coroutineScope)
    }
    private val bluetoothReceiver by lazy {
        HeadsetReceiver(coroutineScope)
    }

    init {
        autoRouteSwitcher()
        coroutineScope.launch {
            headsetReceiver.observeHeadsetEvents().collectLatest {
                when(it) {
                    AudioRouteConstants.Default -> checkAudioRoute()
                    AudioRouteConstants.HeadsetAudio -> audioRouteFlow.emit(it)
                }
            }
        }
        coroutineScope.launch {
            bluetoothReceiver.observeHeadsetEvents().collectLatest {
                when(it) {
                    AudioRouteConstants.Default -> checkAudioRoute()
                    AudioRouteConstants.BluetoothAudio -> audioRouteFlow.emit(it)
                }
            }
        }
    }

    override fun registerAudioControllerReceivers() {
        val headsetFilter = IntentFilter().apply {
            addAction(Intent.ACTION_HEADSET_PLUG)
        }
        val bluetoothFilter = IntentFilter().apply {
            addAction(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED)
        }
        applicationContext?.registerReceiver(headsetReceiver, headsetFilter)
        applicationContext?.registerReceiver(bluetoothReceiver, bluetoothFilter)
    }

    override fun unregisterAudioControllerReceivers() {
        applicationContext?.unregisterReceiver(headsetReceiver)
        applicationContext?.unregisterReceiver(bluetoothReceiver)
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
                if (checkIfSpeakerOn()) {
                    switchAudioToSpeaker()
                }
            }
        }
    }
}