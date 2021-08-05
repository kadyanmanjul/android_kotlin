package com.joshtalks.joshskills.ui.voip.state_management

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.util.Log


class WiredHeadsetReceiver : BroadcastReceiver() {
    private val TAG = "WiredHeadsetReceiver"

    override fun onReceive(context: Context?, intent: Intent?) {
        if (AudioManager.ACTION_HEADSET_PLUG.equals(intent?.action)) {
            Log.d(TAG, "mReceiver: ACTION_HEADSET_PLUG")
            if (intent?.getIntExtra("state", 0) == 1) {
                Log.i(TAG, "Headset plug in")
            } else {
                Log.i(TAG, "Headset plug out")
            }
        }
    }
}