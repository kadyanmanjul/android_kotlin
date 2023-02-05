package com.joshtalks.joshskills.premium.base

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.joshtalks.joshskills.premium.core.LAST_TIME_WORK_MANAGER_START
import com.joshtalks.joshskills.premium.core.PrefManager
import com.joshtalks.joshskills.premium.base.BROADCAST_EVENT_NAME
import com.joshtalks.joshskills.premium.repository.local.AppDatabase
import com.joshtalks.joshskills.premium.repository.local.entity.BroadCastEvent
import com.joshtalks.joshskills.premium.repository.local.model.Mentor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BroadcastHandlerWorker(var context: Context, workerParams: WorkerParameters) :
    CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        if (System.currentTimeMillis() - PrefManager.getLongValue(
                LAST_TIME_WORK_MANAGER_START
            ) >= 30 * 60 * 1000L
        ) {
            PrefManager.put(LAST_TIME_WORK_MANAGER_START, System.currentTimeMillis())
            CoroutineScope(Dispatchers.IO).launch {
                AppDatabase.getDatabase(context)?.broadcastDao()
                    ?.insertBroadcastEvent(
                        BroadCastEvent(
                            Mentor.getInstance().getId(),
                            inputData.getString(BROADCAST_EVENT_NAME)
                        )
                    )
            }
        }
        return Result.success()
    }
}