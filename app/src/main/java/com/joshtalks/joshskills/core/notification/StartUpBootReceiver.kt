package com.joshtalks.joshskills.core.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.JoshApplication
import timber.log.Timber

class StartUpBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Timber.tag(StartUpBootReceiver::class.java.name).e("onReceive")
        if (context is JoshApplication) {
            AppObjectController.init(context)
        }
    }
}