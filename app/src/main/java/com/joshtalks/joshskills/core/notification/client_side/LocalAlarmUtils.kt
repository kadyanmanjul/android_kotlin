package com.joshtalks.joshskills.core.notification.client_side

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.FirebaseRemoteConfigKey
import com.joshtalks.joshskills.core.IS_COURSE_BOUGHT
import com.joshtalks.joshskills.core.PrefManager
import com.joshtalks.joshskills.core.analytics.LogException.catchException
import com.joshtalks.joshskills.core.notification.ScheduledLocalNotificationReceiver

object LocalAlarmUtils {

    fun scheduleNotifications(context:Context,delay :Long= 2000) {
        try {
            if (getNotificationCondition()) {
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
                AlarmUtil(context).createAlarm(pendingIntent, AlarmFrequency.DAILY_AT, delay)
            }
        }catch (ex:Exception){
            catchException(ex)
        }
    }

    private fun getNotificationCondition(): Boolean {
        return  AppObjectController.getFirebaseRemoteConfig().getBoolean(FirebaseRemoteConfigKey.CLIENT_NOTIFICATION_FOR_PAID)
            .xor(PrefManager.getBoolValue(IS_COURSE_BOUGHT)
            ).not() && AppObjectController.getFirebaseRemoteConfig().getBoolean(FirebaseRemoteConfigKey.IS_CLIENT_NOTIFICATION_ACTIVE)
    }

    fun removeLocalNotifications(context:Context) {
        try {
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
        }catch (ex:Exception){
            catchException(ex)
        }
    }
}