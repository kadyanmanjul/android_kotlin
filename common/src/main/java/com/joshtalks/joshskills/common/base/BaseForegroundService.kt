package com.joshtalks.joshskills.common.base

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.joshtalks.joshskills.common.voip.notification.NotificationData
import com.joshtalks.joshskills.common.voip.notification.NotificationPriority
import com.joshtalks.joshskills.common.voip.notification.VoipNotification

class BaseForegroundService : Service() {
    // For Testing Purpose
    private val notificationData = object : NotificationData {
        override fun setTitle(): String {
            return "Checking New Messages"
        }

        override fun setContent(): String {
            return ""
        }

    }
    private val notification by lazy { VoipNotification(notificationData, NotificationPriority.Low) }

    override fun onCreate() {
        super.onCreate()
        showNotification()
    }

    override fun onBind(p0: Intent?): IBinder? { return null }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return super.onStartCommand(intent, flags, startId)
    }

    private fun showNotification() {
        startForeground(
            notification.getNotificationId(),
            notification.getNotificationObject().build()
        )
    }
}