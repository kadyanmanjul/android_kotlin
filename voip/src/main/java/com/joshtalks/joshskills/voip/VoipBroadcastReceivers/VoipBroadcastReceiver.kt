package com.joshtalks.joshskills.voip.VoipBroadcastReceivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

object VoipBroadcastReceiver {

    class P2PNotificationHiddenBroadcastReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            Log.d("TAG", "onReceive: lololol")
        }
    }

}