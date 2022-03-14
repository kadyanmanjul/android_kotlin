package com.joshtalks.joshskills.ui.voip.new_arch.audio_controller

import android.bluetooth.BluetoothHeadset
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import kotlinx.coroutines.flow.MutableSharedFlow
/**
 * add manifest permission-
<uses-permission android:name="android.permission.BLUETOOTH"/>
<uses-permission android:name="android.permission.BLUETOOTH_CONNECT"/>
<uses-permission android:name="android.permission.MODIFY_AUDIO_SETTINGS"/>


 * Add <receiver> -
 <receiver android:name=".ui.voip.audio_controller.HeadsetReceiver"
android:exported="false">
<intent-filter>
<action android:name="android.intent.action.HEADSET_PLUG"/>
</intent-filter>
</receiver>
<receiver android:name=".ui.voip.audio_controller.BluetoothReceiver"
android:exported="false">
<intent-filter>
<action android:name="android.media.ACTION_CONNECTION_STATE_CHANGED"/>
</intent-filter>
</receiver>


 * Register Broadcast Receiver first - AudioController(this).registerAudioControllerReceivers()

 * for speaker while headset/Bluetooth plugin- do like after audio route callbacks-

//is AudioRouteConstants.HeadsetAudio -> {
//Log.d(TAG, "AudioRoute: HeadsetAudio ")
//if(AudioController(this@MainActivity).checkIfSpeakerOn())
//{
//AudioController(this@MainActivity).switchAudioToSpeaker()
//}
//}

 */

class AudioController(private val applicationContext: Context) : AudioControllerInterface {


    private val audioManager = applicationContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager?


    override fun checkIfSpeakerOn(): Boolean {
        return audioManager?.isSpeakerphoneOn ?: false
    }

    override fun checkIfHeadsetPluggedIn(): Boolean {
        return audioManager?.isWiredHeadsetOn ?: false
    }

    override fun checkIfBluetoothConnect(): Boolean {
        return audioManager?.isBluetoothScoOn ?: false
    }

    override fun registerAudioControllerReceivers() {
        val receiverFilter1 = IntentFilter().apply {
            addAction(Intent.ACTION_HEADSET_PLUG)
        }
        val receiverFilter2 = IntentFilter().apply {
            addAction(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED)
        }
        applicationContext.registerReceiver(HeadsetReceiver(), receiverFilter1)
        applicationContext.registerReceiver(BluetoothReceiver(), receiverFilter2)
    }

    override fun observeAudioRoute(): MutableSharedFlow<AudioRouteConstants> {
        return audioRouteFlow
    }

    override fun switchAudioToSpeaker() {
        audioManager?.mode = AudioManager.MODE_NORMAL
        audioManager?.isSpeakerphoneOn = true
    }

    override fun switchAudioToEarpiece() {
        audioManager?.mode = AudioManager.MODE_IN_COMMUNICATION
        audioManager?.isSpeakerphoneOn = false
    }

}