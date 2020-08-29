package com.joshtalks.joshskills.ui.reminder.set_reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Vibrator


class AlarmNotifDismissReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, mIntent: Intent?) {
        stopRingtone(context)
    }


    private fun stopRingtone(context: Context) {
        val mAudioPlayer = com.joshtalks.joshskills.util.RingtoneManager.getInstance(context)
        mAudioPlayer?.stopRingtone()

        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        vibrator.cancel()
    }
}