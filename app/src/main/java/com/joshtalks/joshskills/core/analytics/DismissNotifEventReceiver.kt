package com.joshtalks.joshskills.core.analytics

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.firestore.NotificationAnalytics
import com.joshtalks.joshskills.core.notification.HAS_NOTIFICATION
import com.joshtalks.joshskills.core.notification.NOTIFICATION_ID
import com.joshtalks.joshskills.core.notification.model.NotificationEvent
import com.joshtalks.joshskills.repository.service.EngagementNetworkHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

class DismissNotifEventReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, mIntent: Intent?) {
        Timber.d("Notification Dismissed ${mIntent?.extras?.get("extras")}")
        try {
            if (mIntent != null && mIntent.hasExtra(HAS_NOTIFICATION) && mIntent.hasExtra(
                    NOTIFICATION_ID
                ) && mIntent.getStringExtra(NOTIFICATION_ID).isNullOrEmpty().not()
            ) {
                EngagementNetworkHelper.seenNotificationAndDismissed(
                    mIntent.getStringExtra(
                        NOTIFICATION_ID
                    )
                )
                CoroutineScope(Dispatchers.IO).launch {
                    val notificationId = mIntent.getStringExtra(NOTIFICATION_ID)!!
                    val event = NotificationAnalytics().getNotification(notificationId)?.filter { it.action == "received" }?.get(0)
                    event?.platform?.let { channel ->
                        NotificationAnalytics().addAnalytics(notificationId = notificationId, event = "Dismissed",channel = channel)
                    }
                }
            }
        } catch (ex: Throwable) {
            LogException.catchException(ex)
        }
    }
}
