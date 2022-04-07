package com.joshtalks.joshskills.voip.audiocontroller

import android.annotation.SuppressLint
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.util.Log
import com.joshtalks.joshskills.voip.audiocontroller.AudioRouteConstants.BluetoothAudio
import com.joshtalks.joshskills.voip.audiocontroller.AudioRouteConstants.Default
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch

internal class HeadsetReceiver(val coroutineScope : CoroutineScope) : BroadcastReceiver() {
    private val audioRouteFlow = MutableSharedFlow<AudioRouteConstants>()

    fun observeHeadsetEvents() : SharedFlow<AudioRouteConstants> {
        return audioRouteFlow
    }

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
                coroutineScope.launch {
                    audioRouteFlow.emit(Default)
                }
            }
        }
    }
}

internal class BluetoothReceiver(val coroutineScope : CoroutineScope) : BroadcastReceiver() {
    private val audioRouteFlow = MutableSharedFlow<AudioRouteConstants>()

    fun observeBluetoothEvents() : SharedFlow<AudioRouteConstants> {
        return audioRouteFlow
    }
    override fun onReceive(context: Context?, intent: Intent?) {
        Log.d(TAG, "onReceive: bluetooth")
        when (intent?.getIntExtra(BluetoothProfile.EXTRA_STATE, -1)) {
            BluetoothProfile.STATE_CONNECTED -> {
                coroutineScope.launch {
                    audioRouteFlow.emit(BluetoothAudio)
                }
            }
            BluetoothProfile.STATE_DISCONNECTED -> {
                coroutineScope.launch {
                    audioRouteFlow.emit(Default)
                }
            }
        }
    }
}