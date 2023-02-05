package com.joshtalks.joshskills.premium.core.analytics

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.joshtalks.joshskills.premium.core.firestore.NotificationAnalytics
import com.joshtalks.joshskills.premium.core.notification.HAS_NOTIFICATION
import com.joshtalks.joshskills.premium.core.notification.NOTIFICATION_ID
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
                CoroutineScope(Dispatchers.IO).launch {
                    NotificationAnalytics().addAnalytics(
                        notificationId = mIntent.getStringExtra(NOTIFICATION_ID)!!,
                        mEvent = NotificationAnalytics.Action.DISCARDED,
                        channel = null
                    )
                }
            }
        } catch (ex: Throwable) {
            LogException.catchException(ex)
        }
    }
}
