package com.joshtalks.joshskills.premium.core.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.work.Data
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import com.joshtalks.joshskills.premium.base.BROADCAST_EVENT_NAME
import com.joshtalks.joshskills.premium.base.BroadcastHandlerWorker
import com.joshtalks.joshskills.premium.base.SAVE_BROADCAST_EVENT

class ServiceStartReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        try {
            if (Build.VERSION.SDK_INT < 31) {
                val serviceIntent = Intent(context, BroadcastReceiver::class.java)
                serviceIntent.action = SAVE_BROADCAST_EVENT
                context.startService(serviceIntent)
            } else {
                val broadcastWork = OneTimeWorkRequest.Builder(BroadcastHandlerWorker::class.java)
                val data = Data.Builder()
                data.putString(BROADCAST_EVENT_NAME, intent.action)
                broadcastWork.setInputData(data.build())
                WorkManager.getInstance(context).enqueue(broadcastWork.build())
            }
        } catch (ex: Throwable) {
            ex.printStackTrace()
        }
    }
}