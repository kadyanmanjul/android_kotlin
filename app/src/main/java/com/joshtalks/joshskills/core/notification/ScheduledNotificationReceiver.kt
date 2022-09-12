package com.joshtalks.joshskills.core.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.joshtalks.joshskills.core.firestore.NotificationAnalytics
import com.joshtalks.joshskills.repository.local.AppDatabase
import com.joshtalks.joshskills.repository.local.model.NotificationAction
import com.joshtalks.joshskills.repository.local.model.NotificationObject
import kotlinx.coroutines.*

class ScheduledNotificationReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        val notificationId = intent?.extras?.get("id") as String
        context?.let {
            CoroutineScope(Dispatchers.IO).launch {
                val nc = AppDatabase.getDatabase(context)?.scheduleNotificationDao()?.getNotification(notificationId)
                AppDatabase.getDatabase(context)?.scheduleNotificationDao()?.updateShown(notificationId)
                NotificationUtils(it).sendNotification(NotificationObject().apply {
                    id = nc?.id
                    contentTitle = nc?.title
                    contentText = nc?.body
                    action = NotificationAction.getEnumFromValue(nc?.action)
                    actionData = nc?.action_data
                })
                NotificationAnalytics().addAnalytics(
                    notificationId = nc?.id.toString(),
                    mEvent = NotificationAnalytics.Action.DISPLAYED,
                    channel = NotificationAnalytics.Channel.CLIENT
                )
            }
        }
    }
}