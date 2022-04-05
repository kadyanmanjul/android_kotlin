package com.joshtalks.badebhaiya.core

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Parcelable
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.os.bundleOf
import com.joshtalks.badebhaiya.R
import com.joshtalks.badebhaiya.profile.ProfileActivity
import kotlinx.android.parcel.Parcelize

@Parcelize
data class Notification(
    val title: String,
    val body: String,
    val id: Int,
    val userId: String,
    val type: NotificationType
) : Parcelable

enum class NotificationType(val value: String) {
    REMINDER("Reminders")
}

class NotificationHelper : BroadcastReceiver() {

    companion object {
        const val NOTIFICATION_ID = "notification-id"
        const val NOTIFICATION = "notification"
        const val NOTIFICATION_BUNDLE = "notification_bundle"

        fun getNotificationIntent(context: Context, notification: Notification): Intent =
            Intent(context, NotificationHelper::class.java).apply {
                putExtra(
                    NOTIFICATION_BUNDLE,
                    bundleOf(NOTIFICATION to notification)
                )
            }.also {
                Log.d("NotificationHelper.kt", "YASH => getNotificationIntent: ${it.extras}")
            }

        fun getNotification(
            context: Context,
            channelId: String,
            title: String,
            message: String,
            bigText: String = message,
            autoCancel: Boolean = false,
            contentIntent: PendingIntent? = null
        ): NotificationCompat.Builder =
            NotificationCompat.Builder(context, channelId).apply {
                setSmallIcon(R.drawable.adaptive_icon_foreground)
                setContentTitle(title)
                setContentText(message)
                setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
                priority = NotificationCompat.PRIORITY_MAX
                setContentIntent(contentIntent)
                setAutoCancel(autoCancel)
            }

        fun createNotificationChannel(
            context: Context,
            importance: Int,
            showBadge: Boolean,
            name: String,
            description: String,
            enableLights: Boolean = true,
            enableVibration: Boolean = true
        ) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(name, name, importance)
                channel.description = description
                channel.setShowBadge(showBadge)
                channel.enableLights(enableLights)
                channel.enableVibration(enableVibration)
                val notificationManager = context.getSystemService(NotificationManager::class.java)
                notificationManager.createNotificationChannel(channel)
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        val notificationManager: NotificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        intent.getBundleExtra(NOTIFICATION_BUNDLE)?.getParcelable<Notification>(NOTIFICATION)
            ?.let { notification ->
                notificationManager.notify(
                    notification.id,
                    getNotification(
                        context = context,
                        channelId = notification.type.value,
                        title = notification.title,
                        message = notification.body,
                        autoCancel = false,
                        contentIntent = PendingIntent.getActivity(
                            context,
                            notification.id,
                            Intent(context, ProfileActivity::class.java).apply {
                                putExtra(USER_ID, notification.userId)
                            },
                            PendingIntent.FLAG_UPDATE_CURRENT
                        )
                    ).build()
                )
            }
//        val id: Int = intent.getIntExtra(NOTIFICATION_ID, 0)
//        notificationManager.notify(id, notification)
    }
}