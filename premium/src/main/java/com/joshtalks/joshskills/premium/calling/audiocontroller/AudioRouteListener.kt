package com.joshtalks.joshskills.premium.calling.audiocontroller

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothHeadset
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import org.slf4j.event.LoggingEvent

class AudioRouteListener(val coroutineScope: CoroutineScope, val applicationContext: Context) :
    IAudioRouteListener {
    private val TAG = "AudioRouteListener"
    private val headsetReceiver by lazy { HeadsetReceiver(coroutineScope) }
    private val bluetoothReceiver by lazy { BluetoothReceiver(coroutineScope) }
    private var onRouteChange: (() -> Unit)? = null
    private var audioState = AudioState()


    init { observeAudioRouteReceivers() }

    private fun observeAudioRouteReceivers() {
        coroutineScope.launch {
            try {
                headsetReceiver.observeHeadsetEvents().collect {
                    try {
                        when (it) {
                            AudioRouteConstants.Default -> {
                                audioState = audioState.copy(isHeadsetOn = false)
                                onRouteChange?.invoke()
                            }
                            AudioRouteConstants.HeadsetAudio -> {
                                audioState = audioState.copy(isHeadsetOn = true)
                                onRouteChange?.invoke()
                            }
                            else -> {}
                        }

                    } catch (e: Exception) {
                        if (e is CancellationException)
                            throw e
                        e.printStackTrace()
                    }
                }
            } catch (e: Exception) {
                if (e is CancellationException)
                    throw e
                e.printStackTrace()
            }

        }
        coroutineScope.launch {
            try {
                bluetoothReceiver.observeBluetoothEvents().collect {
                    try {
                        when (it) {
                            AudioRouteConstants.BluetoothAudio -> {
                                audioState = audioState.copy(isBluetoothOn = true)
                                onRouteChange?.invoke()
                            }
                            AudioRouteConstants.Default -> {
                                audioState = audioState.copy(isBluetoothOn = false)
                                onRouteChange?.invoke()
                            }
                            else -> {}
                        }
                    } catch (e: Exception) {
                        if (e is CancellationException)
                            throw e
                        e.printStackTrace()
                    }
                }
            } catch (e: Exception) {
                if (e is CancellationException)
                    throw e
                e.printStackTrace()
            }

        }
    }

    override fun registerAudioControllerReceivers() {
        try {
            val headsetFilter = IntentFilter().apply {
                addAction(Intent.ACTION_HEADSET_PLUG)
            }
            val bluetoothFilter = IntentFilter().apply {
                addAction(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED)
            }
            applicationContext?.registerReceiver(headsetReceiver, headsetFilter)
            applicationContext?.registerReceiver(bluetoothReceiver, bluetoothFilter)
        }catch (ex:Exception){
            Log.d(TAG, "registerAudioControllerReceivers: ${ex.message}")
        }
    }

    override fun unregisterAudioControllerReceivers() {
        try {
            applicationContext?.unregisterReceiver(headsetReceiver)
            applicationContext?.unregisterReceiver(bluetoothReceiver)
        }catch (ex:Exception){
            Log.d(TAG, "unregisterAudioControllerReceivers: ${ex.message}")
        }
    }

    override fun getCurrentAudioRoute(): AudioRouteConstants {
        Log.d(
            TAG,
            "observeAudioRoute getCurrentAudioRoute $audioState"
        )
        if (audioState.isSpeakerOn) {
            return AudioRouteConstants.SpeakerAudio
        }
        if (audioState.isBluetoothOn) {
            return AudioRouteConstants.BluetoothAudio
        }
        if (audioState.isHeadsetOn) {
            return AudioRouteConstants.HeadsetAudio
        }
        // If on call Default will be earpiece or else speaker
        return AudioRouteConstants.Default
    }

    override fun changeSpeakerState(isSpeakerOn: Boolean) {
        audioState = audioState.copy(isSpeakerOn = isSpeakerOn)
    }

    override fun refresh(audioManager: AudioManager?) {
        audioState = AudioState(
            isBluetoothOn = isBluetoothHeadsetConnected(),
                    isHeadsetOn = audioManager?.isWiredHeadsetOn ?: false,
                    isSpeakerOn = false,
                    isDefaultOn = true
        )
    }

    override fun setRouteListener(function: () -> Unit) {
        onRouteChange = function
    }

    @SuppressLint("MissingPermission")
    private fun isBluetoothHeadsetConnected(): Boolean {
        val mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        return (mBluetoothAdapter != null && mBluetoothAdapter.isEnabled
                && mBluetoothAdapter.getProfileConnectionState(BluetoothHeadset.HEADSET) == BluetoothHeadset.STATE_CONNECTED)

    }
}


private data class AudioState(val isBluetoothOn: Boolean = false,
                         val isHeadsetOn: Boolean = false,
                         val isSpeakerOn: Boolean = false,
                         var isDefaultOn: Boolean = false)