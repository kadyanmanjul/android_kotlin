package com.joshtalks.joshskills.common.core.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.joshtalks.joshskills.common.core.AppObjectController
import timber.log.Timber

class StartUpBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Timber.tag(StartUpBootReceiver::class.java.name).e("onReceive")
        AppObjectController.init()
    }
}