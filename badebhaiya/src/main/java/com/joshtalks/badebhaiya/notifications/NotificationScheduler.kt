package com.joshtalks.badebhaiya.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import com.joshtalks.badebhaiya.core.Notification
import com.joshtalks.badebhaiya.core.NotificationHelper
import com.joshtalks.badebhaiya.core.NotificationType
import com.joshtalks.badebhaiya.feed.model.RoomListResponseItem
import com.joshtalks.badebhaiya.utils.datetimeutils.minutesToMilliseconds
import com.joshtalks.badebhaiya.utils.urlToBitmap
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
    This Class is responsible to Schedule notifications.
 */

class NotificationScheduler @Inject constructor(
    @ApplicationContext
    val applicationContext: Context
) {

    private var pendingIntent: PendingIntent? = null

    fun scheduleNotificationAsListener(activity: AppCompatActivity, room: RoomListResponseItem){
        val alarmManager = activity.getSystemService(AppCompatActivity.ALARM_SERVICE) as AlarmManager?

        CoroutineScope(Dispatchers.IO).launch {

            pendingIntent = getPendingIntent(activity, room, NotificationType.LIVE)

            // Schedule notification for exact time
            alarmManager?.setExact(AlarmManager.RTC_WAKEUP, room.startTime!!, pendingIntent)

            // Schedule notification for 5 Minutes Prior

            // Schedule notification for 15 Minutes Prior
            alarmManager?.setExact(AlarmManager.RTC_WAKEUP, room.startTime!! - 10.minutesToMilliseconds(), getPendingIntent(activity, room, NotificationType.REMINDER, priorTo = "10 Mins"))

            alarmManager?.setExact(AlarmManager.RTC_WAKEUP, room.startTime!! - 5.minutesToMilliseconds(), getPendingIntent(activity, room, NotificationType.REMINDER, priorTo = "5 Mins"))

        }
    }

    fun scheduleNotificationsForSpeaker(activity: AppCompatActivity, room: RoomListResponseItem){
        val alarmManager = activity.getSystemService(AppCompatActivity.ALARM_SERVICE) as AlarmManager?

        CoroutineScope(Dispatchers.IO).launch {

            pendingIntent = getPendingIntent(activity, room, NotificationType.LIVE)

            // Schedule notification for exact time
            alarmManager?.setExact(AlarmManager.RTC_WAKEUP, room.startTime!!, pendingIntent)

            // Schedule notification for 5 Minutes Prior

            // Schedule notification for 15 Minutes Prior
            val alarmManager2 = activity.getSystemService(AppCompatActivity.ALARM_SERVICE) as AlarmManager?
            val alarmManager3 = activity.getSystemService(AppCompatActivity.ALARM_SERVICE) as AlarmManager?

            alarmManager2?.setExact(AlarmManager.RTC_WAKEUP, room.startTime!! - 15.minutesToMilliseconds(), getPendingIntent(activity, room, NotificationType.REMINDER, priorTo = "15 Mins"))

            alarmManager3?.setExact(AlarmManager.RTC_WAKEUP, room.startTime!! - 5.minutesToMilliseconds(), getPendingIntent(activity, room, NotificationType.REMINDER, priorTo = "5 Mins"))

        }
    }

    fun cancelScheduledNotification(){
    }

    private suspend fun getPendingIntent(activity: AppCompatActivity, room: RoomListResponseItem, notificationType: NotificationType, priorTo: String? = null): PendingIntent {

        val notificationIntent = createNotificationIntent(activity, room, notificationType, priorTo)

        Timber.d("NOTIFICATION INTENT => ${notificationIntent.extras}")
        return PendingIntent.getBroadcast(
                activity,
            System.currentTimeMillis().toInt(),
                notificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT
            )
        Timber.d("Timer by network => ${room.startTime}")
    }

    private suspend fun createNotificationIntent(activity: AppCompatActivity, room: RoomListResponseItem, notificationType: NotificationType, priorTo: String? = null): Intent{
        val speakerBitmap = room.speakersData?.photoUrl?.urlToBitmap()

        return NotificationHelper.getNotificationIntent(
            activity, Notification(
                title = room.speakersData?.fullName ?: "Conversation Room Reminder",
                body = room.topic ?: "Conversation Room Reminder",
                id = room.startedBy ?: 0,
                userId = room.speakersData?.userId ?: "",
                type = notificationType,
                roomId = room.roomId.toString(),
                speakerBitmap,
                remainingTime = priorTo
            )
        )
    }

}

