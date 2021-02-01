package com.joshtalks.joshskills.core

import android.content.*
import android.os.StrictMode
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.multidex.MultiDexApplication
import com.freshchat.consumer.sdk.Freshchat
import com.joshtalks.joshskills.BuildConfig
import com.joshtalks.joshskills.core.service.NetworkChangeReceiver
import com.joshtalks.joshskills.core.service.WorkManagerAdmin
import io.github.inflationx.viewpump.ViewPumpContextWrapper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

const val TAG = "JoshSkill"

class JoshApplication : MultiDexApplication(), LifecycleObserver,
    ComponentCallbacks2/*, Configuration.Provider*/ {

    companion object {
        @Volatile
        var isAppVisible = false
    }

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        base.let { ViewPumpContextWrapper.wrap(it) }
    }

    override fun onCreate() {
        super.onCreate()
        turnOnStrictMode()
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        AppObjectController.init(this)
        registerBroadcastReceiver()
    }

    override fun onTerminate() {
        super.onTerminate()
        getLocalBroadcastManager().unregisterReceiver(restoreIdReceiver)
        getLocalBroadcastManager().unregisterReceiver(unreadCountChangeReceiver)
    }

    private fun turnOnStrictMode() {
        if (BuildConfig.DEBUG) {
            StrictMode.setThreadPolicy(
                StrictMode.ThreadPolicy.Builder()
                    .penaltyLog()
                    .penaltyDialog()
                    .detectAll()
                    .build()
            )
            StrictMode.setVmPolicy(
                StrictMode.VmPolicy.Builder()
                    .detectLeakedSqlLiteObjects()
                    .detectLeakedClosableObjects()
                    .penaltyLog()
                    .detectAll()
                    .build()
            )
        }
    }

    private fun registerBroadcastReceiver() {
        registerReceiver(
            NetworkChangeReceiver(),
            IntentFilter("android.net.conn.CONNECTIVITY_CHANGE")
        )
        JoshSkillExecutors.BOUNDED.submit {
            if (PrefManager.getStringValue(RESTORE_ID).isBlank()) {
                val intentFilterRestoreID =
                    IntentFilter(Freshchat.FRESHCHAT_USER_RESTORE_ID_GENERATED)
                getLocalBroadcastManager().registerReceiver(
                    restoreIdReceiver,
                    intentFilterRestoreID
                )
            }
        }
        val intentFilterUnreadMessages =
            IntentFilter(Freshchat.FRESHCHAT_UNREAD_MESSAGE_COUNT_CHANGED)
        getLocalBroadcastManager().registerReceiver(
            unreadCountChangeReceiver,
            intentFilterUnreadMessages
        )
    }

    private var restoreIdReceiver: BroadcastReceiver =
        object : BroadcastReceiver() {
            override fun onReceive(
                context: Context,
                intent: Intent
            ) {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val restoreId = AppObjectController.freshChat?.user?.restoreId ?: EMPTY
                        if (restoreId.isBlank().not()) {
                            PrefManager.put(RESTORE_ID, restoreId)
                            val requestMap = mutableMapOf<String, String?>()
                            requestMap["restore_id"] = restoreId
                            AppObjectController.commonNetworkService.postFreshChatRestoreIDAsync(
                                PrefManager.getStringValue(USER_UNIQUE_ID),
                                requestMap
                            )
                        }
                    } catch (ex: Exception) {
                        ex.printStackTrace()
                    }
                }
            }
        }

    private var unreadCountChangeReceiver: BroadcastReceiver =
        object : BroadcastReceiver() {
            override fun onReceive(
                context: Context,
                intent: Intent
            ) {
                AppObjectController.getUnreadFreshchatMessages()
            }
        }

    private fun getLocalBroadcastManager(): LocalBroadcastManager {
        return LocalBroadcastManager.getInstance(this@JoshApplication)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun onAppForegrounded() {
        Timber.tag(TAG).e("************* foregrounded")
        Timber.tag(TAG).e("************* ${isActivityVisible()}")
        isAppVisible = true
        WorkManagerAdmin.userAppUsage(isAppVisible)
        WorkManagerAdmin.userActiveStatusWorker(isAppVisible)
        //  UsageStatsService.activeUserService(this)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun onAppBackgrounded() {
        Timber.tag(TAG).e("************* backgrounded")
        Timber.tag(TAG).e("************* ${isActivityVisible()}")
        isAppVisible = false
        WorkManagerAdmin.userAppUsage(isAppVisible)
        WorkManagerAdmin.userActiveStatusWorker(isAppVisible)
        //  UsageStatsService.inactiveUserService(this)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    fun onAppDestroy() {
        Timber.tag(TAG).e("************* onAppDestroy")
        AppObjectController.releaseInstance()
    }

    private fun isActivityVisible(): String {
        return ProcessLifecycleOwner.get().lifecycle.currentState.name
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        when (level) {
            ComponentCallbacks2.TRIM_MEMORY_RUNNING_MODERATE,
            ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW,
            ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL -> {
                System.runFinalization()
                Runtime.getRuntime().gc()
                System.gc()
            }

            ComponentCallbacks2.TRIM_MEMORY_BACKGROUND,
            ComponentCallbacks2.TRIM_MEMORY_MODERATE,
            ComponentCallbacks2.TRIM_MEMORY_COMPLETE -> {
                return
            }
            else -> {
                return
            }
        }
    }
}
