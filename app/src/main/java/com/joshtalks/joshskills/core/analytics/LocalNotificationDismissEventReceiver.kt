package com.joshtalks.joshskills.core.analytics

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.joshtalks.joshskills.core.LOCAL_NOTIFICATION_INDEX
import com.joshtalks.joshskills.core.PrefManager
import timber.log.Timber

class LocalNotificationDismissEventReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, mIntent: Intent?) {
        Timber.d("Notification Dismissed ${mIntent?.extras?.get("extras")}")
        try {
            PrefManager.put(
                LOCAL_NOTIFICATION_INDEX,
                PrefManager.getIntValue(LOCAL_NOTIFICATION_INDEX, defValue = 0).plus(1)
            )
        } catch (ex: Throwable) {
            LogException.catchException(ex)
        }
    }
}
