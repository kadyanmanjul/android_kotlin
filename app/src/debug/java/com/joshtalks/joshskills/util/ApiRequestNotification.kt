package com.joshtalks.joshskills.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.navigation.NavDeepLinkBuilder
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.common.core.AppObjectController
import com.joshtalks.joshskills.repository.entity.ApiRequest
import com.joshtalks.joshskills.ui.DebugActivity

private const val JOSH_DEV_NOTIFICATION_CHANNEL = "josh_dev_api_requests"

object ApiRequestNotification {
    val context = AppObjectController.joshApplication
    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    const val notificationId = 1069

    fun createNotification(list: List<ApiRequest>) {
        val builder = NotificationCompat.Builder(context, JOSH_DEV_NOTIFICATION_CHANNEL)
        val pendingIntent = NavDeepLinkBuilder(context)
            .setComponentName(DebugActivity::class.java)
            .setGraph(R.navigation.mobile_navigation)
            .setDestination(R.id.apiRequestFragment)
            .createPendingIntent()
        val text = list.joinToString(separator = "\n") { "${it.status}: ${it.method}-${it.url}" }
        builder
            .setContentTitle("API Requests")
            .setContentText("${list.first().status}: ${list.first().method}-${list.first().url}")
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setChannelId(JOSH_DEV_NOTIFICATION_CHANNEL)
            .setOngoing(true)
            .setAutoCancel(false)
            .setContentIntent(pendingIntent)
            .setSmallIcon(R.mipmap.ic_launcher_debug)
        NotificationManagerCompat.from(AppObjectController.joshApplication).notify(notificationId, builder.build())
    }

    fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.createNotificationChannel(NotificationChannel(
                JOSH_DEV_NOTIFICATION_CHANNEL,
                "Josh Talks - API Requests",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Josh Talks - API Requests"
                setShowBadge(false)
                enableLights(false)
                enableVibration(false)
                this.setShowBadge(false)
                this.enableVibration(false)
            })
        }
    }
}