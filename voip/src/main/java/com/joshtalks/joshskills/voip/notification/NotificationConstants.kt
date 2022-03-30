package com.joshtalks.joshskills.voip.notification

import android.app.PendingIntent
import androidx.core.app.NotificationCompat

data class NotificationDetails( val notificationBuilder: NotificationCompat.Builder, val notificationId:Int)
data class NotificationActionObj( val title: String, val actionPendingIntent: PendingIntent)





