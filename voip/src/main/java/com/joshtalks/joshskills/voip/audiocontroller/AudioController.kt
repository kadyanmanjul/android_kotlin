package com.joshtalks.joshskills.voip.audiocontroller

import android.bluetooth.*
import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.util.Log
import com.joshtalks.joshskills.voip.constant.State
import com.joshtalks.joshskills.voip.data.local.PrefManager
import kotlinx.coroutines.CoroutineScope

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

class AudioController(
    val coroutineScope: CoroutineScope,
    val applicationContext: Context,
    val audioRoute: IAudioRouteListener
) : IAudioController {

    private val audioManager =
        applicationContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager?

    override fun activate() {
        audioRoute.refresh(audioManager)
        audioRoute.registerAudioControllerReceivers()
        audioRoute.getCurrentAudioRoute().updateRoute()
        audioRoute.setRouteListener {
            audioRoute.getCurrentAudioRoute().updateRoute()
        }
    }

    override fun switchAudioToSpeaker() {
        Log.d(TAG, "switchAudioToSpeaker: ")
        audioRoute.changeSpeakerState(true)
        audioRoute.getCurrentAudioRoute().updateRoute()
    }

    override fun switchAudioToDefault() {
        Log.d(TAG, "switchAudioToDefault: ")
        audioRoute.changeSpeakerState(false)
        audioRoute.getCurrentAudioRoute().updateRoute()
    }

    override fun deactivate() {
        setToDefault()
    }

    private fun AudioRouteConstants.updateRoute() {
        Log.d(TAG, "updateRoute: $this")
        when (this) {
            AudioRouteConstants.SpeakerAudio -> setSpeakerOn()
            AudioRouteConstants.BluetoothAudio -> setBluetoothOn()
            AudioRouteConstants.HeadsetAudio -> setHeadset()
            AudioRouteConstants.Default -> if (PrefManager.getVoipState() == State.CONNECTED || PrefManager.getVoipState() == State.RECONNECTING)
                setEarpiece()
            else
                setSpeakerOn()
        }
    }

    private fun setToDefault() {
        audioRoute.unregisterAudioControllerReceivers()
        audioRoute.refresh(audioManager)
        audioRoute.getCurrentAudioRoute().updateRoute()
    }

    private fun setSpeakerOn() {
        Log.d(TAG, "setSpeakerOn: ")
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
        Log.d(TAG, "setBluetoothOn: ")
        audioManager?.mode = AudioManager.MODE_IN_COMMUNICATION
        audioManager?.startBluetoothSco()
        audioManager?.isBluetoothScoOn = true
        audioManager?.isSpeakerphoneOn = false
    }

    private fun setHeadset() {
        Log.d(TAG, "setHeadset: ")
        audioManager?.stopBluetoothSco()
        audioManager?.isBluetoothScoOn = false
        audioManager?.isSpeakerphoneOn = false
    }

    private fun setEarpiece() {
        Log.d(TAG, "setEarpiece: ")
        audioManager?.mode = AudioManager.MODE_IN_COMMUNICATION
        audioManager?.stopBluetoothSco()
        audioManager?.isBluetoothScoOn = false
        audioManager?.isSpeakerphoneOn = false
    }
}