package com.joshtalks.joshskills.ui.voip.new_arch.audio_controller

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

var audioRouteFlow = MutableSharedFlow<AudioRouteConstants>()
val coroutineScope = CoroutineScope(Dispatchers.IO)

class HeadsetReceiver : BroadcastReceiver() {
    @SuppressLint("UnsafeProtectedBroadcastReceiver")
    override fun onReceive(context: Context?, intent: Intent?) {
        Log.d(TAG, "onReceive: head")

        when (intent?.getIntExtra("state", 0)) {

            HEADSET_CONNECTED -> {
                coroutineScope.launch {
                    audioRouteFlow.emit(AudioRouteConstants.HeadsetAudio)
                }
            }
            HEADSET_DISCONNECTED -> {
                coroutineScope.launch {
                    audioRouteFlow.emit(AudioRouteConstants.NormalAudio)
                }
            }
        }
    }
}

class BluetoothReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        Log.d(TAG, "onReceive: blue")


        when (intent?.getIntExtra(BluetoothProfile.EXTRA_STATE, -1)) {
            BluetoothProfile.STATE_CONNECTED -> {
                coroutineScope.launch {
                    audioRouteFlow.emit(AudioRouteConstants.BluetoothAudio)

                }
            }
            BluetoothProfile.STATE_DISCONNECTED -> {
                coroutineScope.launch {
                    audioRouteFlow.emit(AudioRouteConstants.NormalAudio)

                }
            }
        }
    }

}