package com.joshtalks.joshskills.core.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.ui.inbox.InboxActivity

class BackgroundService : Service() {

    private val NOTIF_ID = 12301
    private val NOTIF_CHANNEL_ID = "12301"
    private val NOTIF_CHANNEL_NAME = "Background_notif_service"

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground()
        return START_STICKY;
    }

    private fun startForeground() {
        val notificationIntent = Intent(this, InboxActivity::class.java)

        val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

        startForeground(BackgroundService().NOTIF_ID, buildNotification(pendingIntent))
    }

    private fun buildNotification(pendingIntent: PendingIntent): Notification {
        val notificationBuilder = NotificationCompat.Builder(this, BackgroundService().NOTIF_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_status_bar_notification)
                .setOngoing(true)
                .setAutoCancel(false)
                .setContentTitle(getString(R.string.app_name))
                .setContentText("Service is running background")
                .setContentIntent(pendingIntent)
                .setDefaults(Notification.FLAG_ONGOING_EVENT)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            notificationBuilder.priority = NotificationManager.IMPORTANCE_DEFAULT
        }

        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel(
                NOTIF_CHANNEL_ID,
                NOTIF_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            )

            notificationBuilder.setChannelId(NOTIF_CHANNEL_ID)
            notificationManager.createNotificationChannel(notificationChannel)
        }

        val notification = notificationBuilder.build()
        notification.flags = Notification.FLAG_NO_CLEAR

        return notification
    }
}