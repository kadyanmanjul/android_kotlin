package com.joshtalks.joshskills.core.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.joshtalks.joshskills.core.LAST_TIME_WORK_MANAGER_START
import com.joshtalks.joshskills.core.PrefManager
import com.joshtalks.joshskills.base.local.AppDatabase
import com.joshtalks.joshskills.base.local.entity.BroadCastEvent
import com.joshtalks.joshskills.base.local.model.Mentor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

class ServiceStartReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        try {
            if (System.currentTimeMillis() - PrefManager.getLongValue(LAST_TIME_WORK_MANAGER_START) >= 30 * 60 * 1000L) {
                PrefManager.put(LAST_TIME_WORK_MANAGER_START, System.currentTimeMillis())
                CoroutineScope(Dispatchers.IO).launch {
                    Timber.tag("ReceiverCheck").e("${intent.action} : ${intent.dataString}")

                    AppDatabase.getDatabase(context)?.broadcastDao()?.insertBroadcastEvent(
                        BroadCastEvent(
                            Mentor.getInstance().getId(),
                            intent.action
                        )
                    )
                }
            }
        } catch (ex: Throwable) {
            ex.printStackTrace()
        }
    }
}