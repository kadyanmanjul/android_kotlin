package com.joshtalks.joshskills.core.notification

import android.app.Service
import android.content.Intent
import android.os.IBinder
import timber.log.Timber


class AutoStartService : Service() {
    override fun onCreate() {
        super.onCreate()
        Timber.tag(AutoStartService::class.java.name).e("onCreate")
}

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        Timber.tag(AutoStartService::class.java.name).e("onStartCommand")
        return START_STICKY
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onDestroy() {
        Timber.tag(AutoStartService::class.java.name).e("onDestroy")
        val intent = Intent("com.joshtalks.joshskills.start")
        sendBroadcast(intent)
        super.onDestroy()
    }
}
