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
            REMINDER -> getLiveRoomIntent(context, notification.roomId)
            LIVE -> getReminderIntent(context, notification.userId)
        }
    }

    private fun getLiveRoomIntent(context: Context, roomId: String): Intent {
        return FeedActivity.getIntentForNotification(context, roomId = roomId)
    }

    private fun getReminderIntent(context: Context, speakerUserId: String): Intent{
        return FeedActivity.getIntentForProfile(context, speakerUserId)
    }
}