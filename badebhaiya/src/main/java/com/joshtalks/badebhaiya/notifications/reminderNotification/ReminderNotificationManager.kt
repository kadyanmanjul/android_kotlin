package com.joshtalks.badebhaiya.notifications.reminderNotification

import android.content.Context
import android.content.Intent
import com.joshtalks.badebhaiya.core.Notification
import com.joshtalks.badebhaiya.core.NotificationType
import com.joshtalks.badebhaiya.core.NotificationType.*
import com.joshtalks.badebhaiya.feed.FeedActivity

object ReminderNotificationManager {

    fun getRedirectingIntent(context: Context, notification: Notification): Intent{
        return when(notification.type) {
            LIVE -> getLiveRoomIntent(context, notification.roomId, notification.title)
            REMINDER -> getReminderIntent(context, notification.userId)
        }
    }

    private fun getLiveRoomIntent(context: Context, roomId: String, topic: String): Intent {
        return FeedActivity.getIntentForNotification(context, roomId = roomId, topic)
    }

    private fun getReminderIntent(context: Context, speakerUserId: String): Intent{
        return FeedActivity.getIntentForProfile(context, speakerUserId)
    }

}