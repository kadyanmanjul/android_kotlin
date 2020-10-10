package com.joshtalks.joshskills.core.notification

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.joshtalks.joshskills.core.IS_SUBSCRIPTION_STARTED
import com.joshtalks.joshskills.core.IS_TRIAL_STARTED
import com.joshtalks.joshskills.core.PrefManager
import com.joshtalks.joshskills.core.Utils
import com.joshtalks.joshskills.repository.local.model.InstallReferrerModel
import java.util.Calendar
import java.util.Date

class EngageToUseAppNotificationWorker(var context: Context, workerParams: WorkerParameters) :
    CoroutineWorker(context, workerParams) {
    override suspend fun doWork(): Result {
        if (PrefManager.getBoolValue(IS_SUBSCRIPTION_STARTED)) {
            return Result.success()
        }
        if (PrefManager.getBoolValue(IS_TRIAL_STARTED).not()) {
            return Result.success()
        }
        if (isCurrentTimeNotification().not()) {
            return Result.success()
        }
        // today notification limit reached
        val installed: Long = InstallReferrerModel.getPrefObject()?.installOn?.times(1000)
            ?: context.packageManager.getPackageInfo(context.packageName, 0).firstInstallTime

        when (Utils.diffFromToday(Date(installed))) {
            1 -> {

            }
            2 -> {

            }
            3 -> {

            }
            4 -> {

            }
            5 -> {

            }
            6 -> {

            }
            7 -> {

            }
        }






        return Result.success()
    }

    private fun isCurrentTimeNotification(): Boolean {
        val cal: Calendar = Calendar.getInstance()
        cal.time = Date()
        val hour: Int = cal.get(Calendar.HOUR_OF_DAY)
        if (hour in 8..20) {
            return true
        }
        return false
    }
}