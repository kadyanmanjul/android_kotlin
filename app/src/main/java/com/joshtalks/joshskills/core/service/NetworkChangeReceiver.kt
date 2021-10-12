package com.joshtalks.joshskills.core.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.joshtalks.joshskills.base.BaseApplication
import com.joshtalks.joshskills.core.JoshApplication
import com.joshtalks.joshskills.core.Utils

class NetworkChangeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        try {
            if (Utils.isInternetAvailable() && BaseApplication.isAppVisible) {
                WorkManagerAdmin.userActiveStatusWorker(BaseApplication.isAppVisible)
            }
        } catch (ex: Throwable) {
            ex.printStackTrace()
        }
    }

}