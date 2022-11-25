package com.joshtalks.joshskills.common.core.analytics

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.joshtalks.joshskills.common.core.LOCAL_NOTIFICATION_INDEX
import com.joshtalks.joshskills.common.core.PrefManager
import timber.log.Timber

class LocalNotificationDismissEventReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, mIntent: Intent?) {
        Timber.d("Local Notification Dismissed ${mIntent?.extras?.get("extras")}  LOCAL_NOTIFICATION_INDEX: ${PrefManager.getIntValue(LOCAL_NOTIFICATION_INDEX, defValue = 0)}")
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
