package com.joshtalks.joshskills.core.notification.client_side

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.joshtalks.joshskills.core.INACTIVE_DAYS
import com.joshtalks.joshskills.core.PrefManager
import com.joshtalks.joshskills.core.notification.ScheduledLocalNotificationReceiver

object LocalAlarmUtils {

    fun scheduleNotifications(context:Context,delay :Long= 2000) {
        val intent = Intent(context, ScheduledLocalNotificationReceiver::class.java)
        intent.putExtra("id", "local_notification")
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            1030,
            intent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            else
                PendingIntent.FLAG_UPDATE_CURRENT
        )
        AlarmUtil(context).createAlarm(pendingIntent,AlarmFrequency.HOURLY , delay)
    }

    fun removeLocalNotifications(context:Context) {
        PrefManager.put(INACTIVE_DAYS,1)
        val intent = Intent(context, ScheduledLocalNotificationReceiver::class.java)
        intent.putExtra("id", "local_notification")
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            1030,
            intent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            else
                PendingIntent.FLAG_UPDATE_CURRENT
        )
        AlarmUtil(context).deleteAlarm(pendingIntent)
    }
}