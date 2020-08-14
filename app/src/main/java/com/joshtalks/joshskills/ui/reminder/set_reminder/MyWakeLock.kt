package com.joshtalks.joshskills.ui.reminder.set_reminder

import android.content.Context
import android.os.PowerManager


object MyWakeLock {
    private var wakeLock: PowerManager.WakeLock? = null
    fun acquire(c: Context) {
        if (wakeLock != null) wakeLock!!.release()
        val pm =
            c.getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK or
                    PowerManager.ACQUIRE_CAUSES_WAKEUP or
                    PowerManager.ON_AFTER_RELEASE,
            "AlarmManagerJava:MainActivity.APP_TAG"
        )
        wakeLock?.acquire(60 * 1000)
    }

    fun release() {
        if (wakeLock != null) {
            wakeLock!!.release()
        }
        wakeLock = null
    }
}