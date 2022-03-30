package com.joshtalks.joshskills.voip.audiocontroller

import android.annotation.SuppressLint
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch

internal var audioRouteFlow = MutableSharedFlow<AudioRouteConstants>()
private val coroutineScope = CoroutineScope(Dispatchers.IO)

internal class HeadsetReceiver : BroadcastReceiver() {
    @SuppressLint("UnsafeProtectedBroadcastReceiver")
    override fun onReceive(context: Context?, intent: Intent?) {
        Log.d(TAG, "onReceive: headphone")
        when (intent?.getIntExtra("state", 0)) {
            HEADSET_CONNECTED -> {
                coroutineScope.launch {
                    audioRouteFlow.emit(AudioRouteConstants.HeadsetAudio)
                }
            }
            HEADSET_DISCONNECTED -> {
                AudioController().checkAudioRoute()
            }
        }
    }
}

internal class BluetoothReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        Log.d(TAG, "onReceive: bluetooth")
        when (intent?.getIntExtra(BluetoothProfile.EXTRA_STATE, -1)) {
            BluetoothProfile.STATE_CONNECTED -> {
                coroutineScope.launch {
                    audioRouteFlow.emit(AudioRouteConstants.BluetoothAudio)
                }
            }
            BluetoothProfile.STATE_DISCONNECTED -> {
                AudioController().checkAudioRoute()
            }
        }
    }
}