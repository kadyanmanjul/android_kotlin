package com.joshtalks.joshskills.core.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.LAST_TIME_WORK_MANAGER_START
import com.joshtalks.joshskills.core.PrefManager
import com.joshtalks.joshskills.repository.local.model.Mentor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

class ServiceStartReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        try {
            if (System.currentTimeMillis() - PrefManager.getLongValue(LAST_TIME_WORK_MANAGER_START) >= 30*60*1000L) {
                WorkManagerAdmin.setBackgroundServiceWorker()
                CoroutineScope(Dispatchers.IO).launch {
                    Timber.tag("ReceiverCheck").e("${intent.action} : ${intent.dataString}")

                    val request = mapOf(
                        Pair("mentor_id", Mentor.getInstance().getId()),
                        Pair("event_name", intent.action)
                    )
                    AppObjectController.commonNetworkService.saveBroadcastEvent(request)
                }
                PrefManager.put(LAST_TIME_WORK_MANAGER_START, System.currentTimeMillis())
            }
        } catch (ex: Throwable) {
            ex.printStackTrace()
        }
    }
}