package com.joshtalks.joshskills.common.core.notification.client_side

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.joshtalks.joshskills.common.core.AppObjectController
import com.joshtalks.joshskills.common.core.NOTIFICATION_CATEGORY_SCHEDULED
import com.joshtalks.joshskills.common.core.PrefManager
import com.joshtalks.joshskills.common.core.notification.NotificationCategory
import com.joshtalks.joshskills.common.core.notification.ScheduledNotificationReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ClientNotificationUtils(val context: Context) {
    fun updateNotificationDb() {
        CoroutineScope(Dispatchers.IO).launch {
            val categoryMap = PrefManager.getPrefMap(NOTIFICATION_CATEGORY_SCHEDULED) ?: mutableMapOf()
            PrefManager.putPrefObject(NOTIFICATION_CATEGORY_SCHEDULED, categoryMap)
            when {
                categoryMap.containsKey(NotificationCategory.AFTER_FIVE_MIN_CALL.category) ->
                    updateNotificationDb(NotificationCategory.AFTER_FIVE_MIN_CALL)
                categoryMap.containsKey(NotificationCategory.AFTER_FIRST_CALL.category) ->
                    updateNotificationDb(NotificationCategory.AFTER_FIRST_CALL)
                categoryMap.containsKey(NotificationCategory.AFTER_LOGIN.category) ->
                    updateNotificationDb(NotificationCategory.AFTER_LOGIN)

                else -> updateNotificationDb(NotificationCategory.AFTER_LOGIN)
            }
            if (categoryMap.containsKey(NotificationCategory.PAYMENT_INITIATED.category))
                updateNotificationDb(NotificationCategory.PAYMENT_INITIATED)
            else if (categoryMap.containsKey(NotificationCategory.AFTER_BUY_PAGE.category))
                updateNotificationDb(NotificationCategory.AFTER_BUY_PAGE)
        }
    }

    fun updateNotificationDb(category: NotificationCategory) {
        CoroutineScope(Dispatchers.IO).launch {
            val notificationList =
                AppObjectController.appDatabase.scheduleNotificationDao()
                    .getUnscheduledCatNotifications(category.category)
            val categoryMap = PrefManager.getPrefMap(NOTIFICATION_CATEGORY_SCHEDULED) ?: mutableMapOf()
            categoryMap[category.category] = 1
            PrefManager.putPrefObject(NOTIFICATION_CATEGORY_SCHEDULED, categoryMap)
            notificationList.forEach {
                val intent = Intent(context.applicationContext, ScheduledNotificationReceiver::class.java)
                intent.putExtra("id", it.id)
                val pendingIntent = PendingIntent.getBroadcast(
                    context.applicationContext,
                    it.id.hashCode(),
                    intent,
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                    else
                        PendingIntent.FLAG_UPDATE_CURRENT
                )
                AlarmUtil(context).createAlarm(pendingIntent, it.frequency!!, it.execute_after)
                AppObjectController.appDatabase.scheduleNotificationDao().updateScheduled(it.id)
            }
        }
    }

    fun removeScheduledNotification(category: NotificationCategory) {
        CoroutineScope(Dispatchers.IO).launch {
            val notificationIds =
                AppObjectController.appDatabase.scheduleNotificationDao().removeCategory(category.category)
            notificationIds.forEach {
                val intent = Intent(context.applicationContext, ScheduledNotificationReceiver::class.java)
                intent.putExtra("id", it)
                val pendingIntent =
                    PendingIntent.getBroadcast(
                        context.applicationContext,
                        it.hashCode(),
                        intent,
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                        else
                            PendingIntent.FLAG_UPDATE_CURRENT
                    )
                AlarmUtil(context).deleteAlarm(pendingIntent)
            }
        }
    }

    fun removeAllScheduledNotification() {
        CoroutineScope(Dispatchers.IO).launch {
            removeAllNotificationAsync()
        }
    }

    suspend fun removeAllNotificationAsync() {
        val notificationIds = AppObjectController.appDatabase.scheduleNotificationDao().clearAllNotifications()
        notificationIds.forEach {
            try {
                val intent = Intent(context.applicationContext, ScheduledNotificationReceiver::class.java)
                intent.putExtra("id", it)
                val pendingIntent =
                    PendingIntent.getBroadcast(
                        context.applicationContext,
                        it.hashCode(),
                        intent,
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                        else
                            PendingIntent.FLAG_UPDATE_CURRENT
                    )
                AlarmUtil(context).deleteAlarm(pendingIntent)
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }
}