package com.joshtalks.badebhaiya.notifications

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.core.app.NotificationCompat
import com.joshtalks.badebhaiya.R
import com.joshtalks.badebhaiya.core.Notification
import com.joshtalks.badebhaiya.core.NotificationHelper
import com.joshtalks.badebhaiya.core.NotificationType.*
import com.joshtalks.badebhaiya.notifications.reminderNotification.ReminderNotificationManager
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
  This Class is responsible to shoot notifications.
 */

@Singleton
class NotificationLauncher @Inject constructor(
    @ApplicationContext
    val applicationContext: Context
) {

    private fun notificationManager(context: Context): NotificationManager {
        return context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    private fun launchLiveRoomNotification(context: Context, notificationData: Notification) {

        createNotification(context, notificationData)
    }

    fun launchNotification(context: Context, intent: Intent) {
        val notificationData = getNotificationData(intent)
//        when (notificationData?.type) {
//            REMINDER -> {}
//            LIVE -> launchLiveRoomNotification(context, notificationData)
//            null -> {}
//        }
        createNotification(context, notificationData)
    }

    private fun getNotificationData(intent: Intent): Notification? =
        intent.getBundleExtra(NotificationHelper.NOTIFICATION_BUNDLE)?.getParcelable<Notification>(
            NotificationHelper.NOTIFICATION
        )

    private fun getTitle(notificationData: Notification): String =
        String.format(getCorrespondingTitle(notificationData), notificationData.title)

    private fun getCorrespondingTitle(notificationData: Notification): String =
        when(notificationData.type){
            REMINDER -> ""
            LIVE -> applicationContext.getString(R.string.live_room_notification_title)
        }

    private fun getNotification(
        context: Context,
        channelId: String,
        title: String,
        message: String,
        bigText: String = message,
        autoCancel: Boolean = false,
        profilePicture: Bitmap?,
        contentIntent: PendingIntent? = null
    ): NotificationCompat.Builder =
        NotificationCompat.Builder(context, channelId).apply {
            setSmallIcon(R.drawable.ic_notification_icon)
            setContentTitle(title)
            setContentText(message)
            setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
            priority = NotificationCompat.PRIORITY_MAX
            setContentIntent(contentIntent)
            profilePicture?.let {
                setLargeIcon(it)
            }
            setAutoCancel(autoCancel)
        }

    private fun createNotification(context: Context, notificationData: Notification?) {
        notificationData?.let { notification ->
            Timber.d("Notification aur data hai => $notification")
            notificationManager(context).notify(
                notification.id,
                getNotification(
                    context = context,
                    channelId = notification.type.value,
                    title = getTitle(notification),
                    message = notification.body,
                    autoCancel = false,
                    profilePicture = notification.speakerPicture,
                    contentIntent = PendingIntent.getActivity(
                        context,
                        notification.id,
                        ReminderNotificationManager.getRedirectingIntent(context, notification),
                        PendingIntent.FLAG_UPDATE_CURRENT
                    )
                ).build()
            )
        }
    }
}