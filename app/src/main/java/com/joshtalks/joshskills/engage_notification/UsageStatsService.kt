package com.joshtalks.joshskills.engage_notification

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.Process
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.JoshApplication
import com.joshtalks.joshskills.core.JoshSkillExecutors
import java.util.concurrent.ExecutorService
import timber.log.Timber

class UsageStatsService : Service() {
    private var updateUsageStatsThread: HandlerThread? = null
    private var serviceHandler: Handler? = null
    private var taskExecuteEverySeconds: Runnable? = null
    private var cTime = 0
    private val executor: ExecutorService =
        JoshSkillExecutors.newCachedSingleThreadExecutor("Josh-Usage-Stats-Service")

    override fun onCreate() {
        Timber.tag(TAG).e("onCreate")
        updateUsageStatsThread = HandlerThread(
            UPDATE_USAGE_STATS_THREAD_NAME,
            Process.THREAD_PRIORITY_BACKGROUND
        )
        updateUsageStatsThread?.start()
        updateUsageStatsThread?.looper?.run {
            serviceHandler = Handler(this)
        }
    }


    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        Timber.tag(TAG).e("onStartCommand")
        if (intent.action == null) {
            return START_NOT_STICKY
        }
        executor.execute {
            intent.action?.run {
                Timber.tag(TAG).e(this)
                when (OnInActive().action) {
                    this -> {
                        Timber.tag(TAG).e("inactive")
                        taskExecuteEverySeconds?.run {
                            serviceHandler?.removeCallbacks(this)
                        }
                        AppObjectController.appDatabase.appUsageDao()
                            .insertIntoAppUsage(AppUsageModel(cTime))
                        cTime = 0
                    }
                    else -> {
                        Timber.tag(TAG).e("active")
                        taskExecuteEverySeconds = object : Runnable {
                            override fun run() {
                                if (JoshApplication.isAppVisible) {
                                    ++cTime
                                }
                                serviceHandler?.postDelayed(this, ONE_SECONDS)
                            }
                        }
                        taskExecuteEverySeconds?.run {
                            serviceHandler?.post(this)
                        }
                    }
                }
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent): IBinder? {
        throw UnsupportedOperationException("Not yet implemented")
    }

    override fun onDestroy() {
        super.onDestroy()
        taskExecuteEverySeconds?.run {
            serviceHandler?.removeCallbacks(this)
        }
        updateUsageStatsThread?.quit()
    }

    companion object {
        private const val TAG = "UsageStatsService"
        private const val UPDATE_USAGE_STATS_THREAD_NAME = "UpdateUsageStatsThread"
        private const val ONE_SECONDS: Long = 1000

        fun activeUserService(context: Context) {
            val serviceIntent = Intent(
                context,
                UsageStatsService::class.java
            ).apply {
                action = OnActive().action
            }
            AppObjectController.joshApplication.startService(serviceIntent)
        }

        fun inactiveUserService(context: Context) {
            val serviceIntent = Intent(
                context,
                UsageStatsService::class.java
            ).apply {
                action = OnInActive().action
            }
            AppObjectController.joshApplication.startService(serviceIntent)
        }
    }
}

sealed class AppUsageAction
data class OnActive(val action: String = "appusage.action.active") : AppUsageAction()
data class OnInActive(val action: String = "appusage.action.inactive") : AppUsageAction()

