package com.joshtalks.joshskills.premium.base

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.joshtalks.joshskills.premium.core.LAST_TIME_WORK_MANAGER_START
import com.joshtalks.joshskills.premium.core.PrefManager
import com.joshtalks.joshskills.premium.repository.local.AppDatabase
import com.joshtalks.joshskills.premium.repository.local.entity.BroadCastEvent
import com.joshtalks.joshskills.premium.repository.local.model.Mentor
import com.joshtalks.joshskills.premium.calling.notification.NotificationData
import com.joshtalks.joshskills.premium.calling.notification.NotificationPriority
import com.joshtalks.joshskills.premium.calling.notification.VoipNotification
import kotlinx.coroutines.*
import java.util.concurrent.atomic.AtomicInteger

const val SAVE_BROADCAST_EVENT = "ACTION_SAVE_BROADCAST_EVENT"
const val BROADCAST_EVENT_NAME = "BROADCAST_EVENT_NAME"

class BroadcastHandlerService : Service() {
    private val TAG = "BroadcastHandlerService"
    var jobCounter = AtomicInteger(0)
    private val coroutineExceptionHandler = CoroutineExceptionHandler { _, e ->
        Log.d(TAG, " Coroutine Exception : Handled in CoroutineExceptionHandler")
        e.printStackTrace()
    }
    val scope = CoroutineScope(Dispatchers.IO + coroutineExceptionHandler)
    private val notificationData = object : NotificationData {
        override fun setTitle(): String {
            return "Checking New Messages"
        }

        override fun setContent(): String {
            return ""
        }

    }
    private val notification by lazy {
        VoipNotification(
            notificationData,
            NotificationPriority.Low
        )
    }

    override fun onCreate() {
        super.onCreate()
        showNotification()
    }

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        jobCounter.incrementAndGet()
        when (intent?.action) {
            SAVE_BROADCAST_EVENT -> {
                scope.handleRequest {
                    if (System.currentTimeMillis() - PrefManager.getLongValue(LAST_TIME_WORK_MANAGER_START) >= 30 * 60 * 1000L) {
                        PrefManager.put(LAST_TIME_WORK_MANAGER_START, System.currentTimeMillis())
                        CoroutineScope(Dispatchers.IO).launch {
                            AppDatabase.getDatabase(this@BroadcastHandlerService)?.broadcastDao()
                                ?.insertBroadcastEvent(
                                    BroadCastEvent(
                                        Mentor.getInstance().getId(),
                                        intent.action
                                    )
                                )
                        }
                    }
                }
            }
            else -> {
                serviceStopRequest()
            }
        }
        return START_NOT_STICKY
    }

    private fun serviceStopRequest() {
        if (jobCounter.decrementAndGet() == 0)
            stopSelf()
    }

    private fun showNotification() {
        startForeground(
            notification.getNotificationId(),
            notification.getNotificationObject().build()
        )
    }

    // TODO: Need to improve
    private inline fun CoroutineScope.handleRequest(crossinline block: () -> Unit) {
        try {
            this.launch {
                try {
                    block()
                    serviceStopRequest()
                } catch (e: Exception) {
                    if (e is CancellationException)
                        throw e
                    else
                        e.printStackTrace()
                }
            }
        } catch (e: Exception) {
            if (e is CancellationException)
                throw e
            else
                e.printStackTrace()
        }
    }
}