package com.joshtalks.badebhaiya.core.analytics

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.joshtalks.badebhaiya.core.EngagementNetworkHelper
import com.joshtalks.badebhaiya.core.LogException
import com.joshtalks.badebhaiya.notifications.HAS_NOTIFICATION
import com.joshtalks.badebhaiya.notifications.NOTIFICATION_ID
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
            }
        } catch (ex: Throwable) {
            LogException.catchException(ex)
        }
    }
}
