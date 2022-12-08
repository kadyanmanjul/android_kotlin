package com.joshtalks.joshskills.voip.notification

import android.app.PendingIntent
import androidx.core.app.NotificationCompat

data class NotificationDetails( val notificationBuilder: NotificationCompat.Builder, val notificationId:Int)
data class NotificationActionObj( val title: String, val actionPendingIntent: PendingIntent)
data class NotificationBuiltObj( val id: Int, val notificationBuilder: NotificationCompat.Builder)

sealed class NotificationPriority {
    object High : NotificationPriority()
    object Low : NotificationPriority()
}




