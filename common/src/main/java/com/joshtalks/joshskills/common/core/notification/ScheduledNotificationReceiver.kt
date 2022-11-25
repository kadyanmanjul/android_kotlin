package com.joshtalks.joshskills.common.core.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.joshtalks.joshskills.common.core.BLOCK_STATUS
import com.joshtalks.joshskills.common.core.FREE_TRIAL_TEST_ID
import com.joshtalks.joshskills.common.core.PrefManager
import com.joshtalks.joshskills.common.core.Utils
import com.joshtalks.joshskills.common.core.firestore.NotificationAnalytics
import com.joshtalks.joshskills.common.repository.local.AppDatabase
import com.joshtalks.joshskills.common.repository.local.model.NotificationAction
import com.joshtalks.joshskills.common.repository.local.model.NotificationObject
import com.joshtalks.joshskills.common.ui.voip.new_arch.ui.utils.isBlocked
import kotlinx.coroutines.*

class ScheduledNotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        val notificationId = intent?.extras?.get("id") as String
        context?.let {
            CoroutineScope(Dispatchers.IO).launch {
                val database = AppDatabase.getDatabase(context)
                val nc = database?.scheduleNotificationDao()?.getNotification(notificationId)
                var notificationAction = NotificationAction.getEnumFromValue(nc?.action)
                var ncActionData = nc?.action_data

                try {
                    when (nc?.action) {
                        NotificationAction.INITIATE_RANDOM_CALL.type -> {
                            if (database.courseDao().getMaxExpiryTime().time < System.currentTimeMillis() || isBlocked()) {
                                notificationAction = NotificationAction.ACTION_OPEN_PAYMENT_PAGE
                                ncActionData = Utils.getLangPaymentTestIdFromTestId(
                                    PrefManager.getStringValue(FREE_TRIAL_TEST_ID)
                                )
                            }
                        }
                        NotificationAction.ACTION_OPEN_SPEAKING_SECTION.type -> {
                            if (database.courseDao().getMaxExpiryTime().time < System.currentTimeMillis()) {
                                notificationAction = NotificationAction.ACTION_OPEN_PAYMENT_PAGE
                                ncActionData = Utils.getLangPaymentTestIdFromTestId(
                                    PrefManager.getStringValue(FREE_TRIAL_TEST_ID)
                                )
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                AppDatabase.getDatabase(context)?.scheduleNotificationDao()?.updateShown(notificationId)
                NotificationUtils(it).sendNotification(NotificationObject().apply {
                    id = nc?.id
                    contentTitle = nc?.title
                    contentText = nc?.body
                    action = notificationAction
                    actionData = ncActionData
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