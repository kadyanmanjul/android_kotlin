package com.joshtalks.joshskills.core.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.repository.local.model.Mentor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

class ServiceStartReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        try {
            initService(context)
            CoroutineScope(Dispatchers.IO).launch {
                Timber.tag("ReceiverCheck").e("${intent.action} : ${intent.dataString}")

                val request = mapOf(
                    Pair("mentor_id", Mentor.getInstance().getId()),
                    Pair("event_name", intent.action)
                )
                AppObjectController.commonNetworkService.saveBroadcastEvent(request)
            }
        } catch (ex: Throwable) {
            ex.printStackTrace()
        }
    }

    private fun initService(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.applicationContext.startForegroundService(
                Intent(context.applicationContext, BackgroundService::class.java)
            )
        } else {
            context.applicationContext.startService(
                Intent(context.applicationContext, BackgroundService::class.java)
            )
        }
    }
}