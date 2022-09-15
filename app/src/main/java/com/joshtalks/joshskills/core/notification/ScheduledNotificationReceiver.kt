package com.joshtalks.joshskills.core.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.joshtalks.joshskills.core.BLOCK_STATUS
import com.joshtalks.joshskills.core.FREE_TRIAL_TEST_ID
import com.joshtalks.joshskills.core.PrefManager
import com.joshtalks.joshskills.core.Utils
import com.joshtalks.joshskills.core.firestore.NotificationAnalytics
import com.joshtalks.joshskills.repository.local.AppDatabase
import com.joshtalks.joshskills.repository.local.model.NotificationAction
import com.joshtalks.joshskills.repository.local.model.NotificationObject
import com.joshtalks.joshskills.ui.lesson.speaking.spf_models.BlockStatusModel
import kotlinx.coroutines.*
import java.time.Duration

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

    private fun isBlocked(): Boolean {
        val blockStatus = PrefManager.getBlockStatusObject(BLOCK_STATUS)
        if (blockStatus?.timestamp?.toInt() == 0)
            return false

        if (checkWithinBlockTimer(blockStatus)) {
            return true
        }
        return false
    }

    private fun checkWithinBlockTimer(blockStatus: BlockStatusModel?): Boolean {
        if (blockStatus != null) {
            val durationInMillis = Duration.ofMinutes(blockStatus.duration.toLong()).toMillis()
            val unblockTimestamp = blockStatus.timestamp + durationInMillis
            if (System.currentTimeMillis() <= unblockTimestamp) {
                return true
            }
        }
        return false
    }
}