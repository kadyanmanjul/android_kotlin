package com.joshtalks.joshskills.voip.audiocontroller

import android.annotation.SuppressLint
import android.bluetooth.*
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.util.Log
import com.joshtalks.joshskills.voip.Utils
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
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

private const val TAG = "AudioController"

class AudioController(val coroutineScope: CoroutineScope) : AudioControllerInterface {

    private val applicationContext = Utils.context
    private val audioManager = Utils.context?.getSystemService(Context.AUDIO_SERVICE) as AudioManager?
    private val audioRouteFlow = MutableSharedFlow<AudioRouteConstants>()

    companion object {
        private var isSpeakerOn: Boolean = false
        private var isHeadsetOn: Boolean = false
        private var isBluetoothOn: Boolean = false
    }

    private val headsetReceiver by lazy {
        HeadsetReceiver(coroutineScope)
    }
    private val bluetoothReceiver by lazy {
        BluetoothReceiver(coroutineScope)
    }

    init {
        observeAudioRouteReceivers()
        initAudioRouteVariables()
    }

    private fun observeAudioRouteReceivers() {
        coroutineScope.launch {
            try {
                headsetReceiver.observeHeadsetEvents().collect {
                    try {
                        when (it) {
                            AudioRouteConstants.Default -> {
                                changeRouteStatus(isHeadsetOn = false)
                            }
                            AudioRouteConstants.HeadsetAudio -> {
                                changeRouteStatus(isHeadsetOn = true)
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
                                changeRouteStatus(isBluetoothOn = true)
                            }
                            AudioRouteConstants.Default -> {
                                changeRouteStatus(isBluetoothOn = false)
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

    @SuppressLint("MissingPermission")
    private fun initAudioRouteVariables() {
        Log.d(TAG, "observeAudioRoute initAudioRouteVariables WiredHeadset=${audioManager?.isWiredHeadsetOn} Bluetooth=${isBluetoothHeadsetConnected()}")
        isBluetoothOn = isBluetoothHeadsetConnected()
        isHeadsetOn = audioManager?.isWiredHeadsetOn ?: false
        isSpeakerOn = false
        changeRouteStatus(isBluetoothOn= isBluetoothOn, isHeadsetOn= isHeadsetOn, isSpeakerOn= isSpeakerOn)
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

    override fun observeAudioRoute(): SharedFlow<AudioRouteConstants> {
        return audioRouteFlow
    }

    override fun getCurrentAudioRoute(): AudioRouteConstants {
        Log.d(TAG, "observeAudioRoute getCurrentAudioRoute speaker=$isSpeakerOn Bluetooth=$isBluetoothOn WiredHeadset=$isHeadsetOn ")
        if (isSpeakerOn) {
            setSpeakerOn()
            return AudioRouteConstants.SpeakerAudio
        }
        if (isBluetoothOn) {
            setBluetoothOn()
            return AudioRouteConstants.BluetoothAudio
        }
        if (isHeadsetOn) {
            setHeadset()
            return AudioRouteConstants.HeadsetAudio
        }
        setEarpiece()
        return AudioRouteConstants.EarpieceAudio
    }

    private fun setSpeakerOn() {
        audioManager?.mode = AudioManager.MODE_NORMAL
        audioManager?.stopBluetoothSco()
        audioManager?.isBluetoothScoOn = false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            audioManager?.availableCommunicationDevices?.firstOrNull { it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER }
                ?.let {
                    audioManager.setCommunicationDevice(it)
                }
        }
        audioManager?.isSpeakerphoneOn = true
    }

    private fun setBluetoothOn() {
        audioManager?.mode = AudioManager.MODE_IN_COMMUNICATION
        audioManager?.startBluetoothSco()
        audioManager?.isBluetoothScoOn = true
    }

    private fun setHeadset() {
        audioManager?.stopBluetoothSco()
        audioManager?.isBluetoothScoOn = false
        audioManager?.isSpeakerphoneOn = false
    }

    private fun setEarpiece() {
        audioManager?.mode = AudioManager.MODE_IN_COMMUNICATION
        audioManager?.stopBluetoothSco()
        audioManager?.isBluetoothScoOn = false
        audioManager?.isSpeakerphoneOn = false
    }

    override fun switchAudioToSpeaker() {
        changeRouteStatus(isSpeakerOn = true)
    }

    override fun switchAudioToDefault() {
        changeRouteStatus(isSpeakerOn = false)
    }

    override fun resetAudioRoute() {
        initAudioRouteVariables()
    }

    private fun checkAudioRoute() {
        emitAudioRoute(getCurrentAudioRoute())
    }

    private fun emitAudioRoute(audioRoute: AudioRouteConstants) {
        coroutineScope.launch {
            try {
                audioRouteFlow.emit(audioRoute)
            } catch (e: Exception) {
                if (e is CancellationException)
                    throw e
                e.printStackTrace()
            }
        }
    }

    private fun changeRouteStatus(isBluetoothOn: Boolean? = null, isHeadsetOn: Boolean? = null, isSpeakerOn: Boolean?=null) {
        if (isHeadsetOn != null) {
            Companion.isHeadsetOn = isHeadsetOn
        }
        if (isBluetoothOn != null) {
            Companion.isBluetoothOn = isBluetoothOn
        }
        if (isSpeakerOn != null) {
            Companion.isSpeakerOn = isSpeakerOn
        }
        checkAudioRoute()
    }

    @SuppressLint("MissingPermission")
   private fun isBluetoothHeadsetConnected(): Boolean {
        val mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        return (mBluetoothAdapter != null && mBluetoothAdapter.isEnabled
                && mBluetoothAdapter.getProfileConnectionState(BluetoothHeadset.HEADSET) == BluetoothHeadset.STATE_CONNECTED)

    }
}