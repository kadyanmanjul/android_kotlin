package com.joshtalks.joshskills.core.analytics

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.notification.HAS_NOTIFICATION
import com.joshtalks.joshskills.core.notification.NOTIFICATION_ID
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
                    AppObjectController.appDatabase.notificationDao().updateAction(
                        mIntent.getStringExtra(NOTIFICATION_ID)!!,"Dismissed"
                    )
                }
            }
        } catch (ex: Throwable) {
            LogException.catchException(ex)
        }
    }
}
