package com.joshtalks.badebhaiya.notifications

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import androidx.core.app.NotificationCompat
import com.joshtalks.badebhaiya.R
import com.joshtalks.badebhaiya.core.Notification
import com.joshtalks.badebhaiya.core.NotificationHelper
import com.joshtalks.badebhaiya.core.NotificationType.*
import com.joshtalks.badebhaiya.notifications.reminderNotification.ReminderNotificationManager
import com.joshtalks.badebhaiya.pubnub.PubNubManager
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

    fun launchNotification(context: Context, intent: Intent) {
        val notificationData = getNotificationData(intent)
        createNotification(context, notificationData)
    }

    private fun getNotificationData(intent: Intent): Notification? =
        intent.getBundleExtra(NotificationHelper.NOTIFICATION_BUNDLE)?.getParcelable<Notification>(
            NotificationHelper.NOTIFICATION
        )

    private fun getTitle(notificationData: Notification): String =
        when(notificationData.type){
            REMINDER -> if (notificationData.isSpeaker()) applicationContext.getString(R.string.reminder_for_your_call) else applicationContext.getString(R.string.speakers_call_starts_in, notificationData.speakerName, notificationData.remainingTime)
            LIVE -> String.format(getTitleForLive(), notificationData.title)
        }

    private fun getTitleForLive(): String =
       applicationContext.getString(R.string.live_room_notification_title)


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
            setSmallIcon(R.drawable.ic_status_bar_notification)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                color = applicationContext.getColor(R.color.notification_orange)
            }
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
            if (!PubNubManager.isRoomActive || PubNubManager.getLiveRoomProperties().roomId.toString() != notification.roomId) {
                Timber.d("Notification aur data hai => $notification")
                notificationManager(context).notify(
                    notification.id,
                    getNotification(
                        context = context,
                        channelId = notification.type.value,
                        title = getTitle(notification),
                        message = getBody(notification),
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

    private fun getBody(notificationData: Notification): String {
        return when(notificationData.type){
            REMINDER -> notificationData.remainingTime?.let { remaining ->
                if (notificationData.isSpeaker()){
                    String.format(applicationContext.getString(R.string.your_call_will_start_in, remaining))
                } else {
                    applicationContext.getString(R.string.reminder_notification_body_for_listener)
                }
            } ?: String.format(applicationContext.getString(R.string.your_call_will_start_in, "Soon"))
            LIVE -> String.format(applicationContext.getString(R.string.speak_with_them), notificationData.body)
        }
    }
}