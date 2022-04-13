package com.joshtalks.joshskills.core.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.repository.local.model.Mentor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

class NetworkChangeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        try {
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

}