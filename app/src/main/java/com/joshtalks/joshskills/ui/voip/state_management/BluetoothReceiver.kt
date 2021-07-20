package com.joshtalks.joshskills.ui.voip.state_management

import android.bluetooth.BluetoothHeadset
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED
import android.util.Log


class BluetoothReceiver : BroadcastReceiver() {
    private val TAG = "BluetoothReceiver"

    override fun onReceive(context: Context?, intent: Intent?) {
        // Checking Bluetooth Connection State i.e. Its connected to device or not
        if (intent?.action.equals(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED)) {
            Log.d(TAG, "onReceive: ACTION_CONNECTION_STATE_CHANGED")
            val state = intent?.getIntExtra(BluetoothHeadset.EXTRA_STATE, -1)
            when (state) {
                BluetoothHeadset.STATE_AUDIO_CONNECTED -> Log.d(TAG, "Bluetooth connected")
                BluetoothHeadset.STATE_AUDIO_DISCONNECTED -> Log.d(TAG, "Bluetooth disconnected")
            }
        } else if (intent?.action.equals(ACTION_SCO_AUDIO_STATE_UPDATED)) { // Checking Bluetooth SOC State i.e. Audio is playing through bluetooth
            val state = intent?.getIntExtra(
                AudioManager.EXTRA_SCO_AUDIO_STATE,
                AudioManager.SCO_AUDIO_STATE_ERROR
            )
            when (state) {
                AudioManager.SCO_AUDIO_STATE_CONNECTED -> {
                    Log.d(TAG, "onReceive: SCO_AUDIO_STATE_CONNECTED")
                }
                AudioManager.SCO_AUDIO_STATE_DISCONNECTED -> {
                    Log.d(TAG, "onReceive: SCO_AUDIO_STATE_DISCONNECTED")
                }
                AudioManager.SCO_AUDIO_STATE_CONNECTING -> {
                    Log.d(TAG, "onReceive: SCO_AUDIO_STATE_CONNECTING")
                }
                AudioManager.SCO_AUDIO_STATE_ERROR -> {
                    Log.d(TAG, "onReceive: SCO_AUDIO_STATE_ERROR")
                }
                else -> {
                    Log.d(TAG, "onReceive: ELSE")
                }
            }
        }
    }
}