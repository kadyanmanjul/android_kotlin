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
import com.yariksoffice.lingver.Lingver
import io.github.inflationx.viewpump.ViewPumpContextWrapper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

var TAG = "JoshSkill"

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

    fun turnOnStrictMode() {
        if (BuildConfig.DEBUG) {
            StrictMode.setThreadPolicy(
                StrictMode.ThreadPolicy.Builder()
                    .detectAll()
                    .penaltyLog()
                    .penaltyDeath().build()
            )
            StrictMode.setVmPolicy(
                StrictMode.VmPolicy.Builder()
                    .detectAll()
                    .penaltyLog()
                    .penaltyDeath().build()
            )
        }
    }

    fun permitDiskReads(func: () -> Any): Any {
        if (BuildConfig.DEBUG) {
            val oldThreadPolicy = StrictMode.getThreadPolicy()
            StrictMode.setThreadPolicy(
                StrictMode.ThreadPolicy.Builder(oldThreadPolicy)
                    .permitDiskReads().build()
            )
            val anyValue = func()
            StrictMode.setThreadPolicy(oldThreadPolicy)

            return anyValue
        } else {
            return func()
        }
    }

    override fun onCreate() {
        super.onCreate()
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        AppObjectController.init(this)
        if (PrefManager.getStringValue(USER_LOCALE).isEmpty()) {
            PrefManager.put(USER_LOCALE, "en")
        }
        Lingver.init(this, PrefManager.getStringValue(USER_LOCALE))
        registerBroadcastReceiver()
    }

    private fun registerBroadcastReceiver() {
        registerReceiver(
            NetworkChangeReceiver(),
            IntentFilter("android.net.conn.CONNECTIVITY_CHANGE")
        )

        if (PrefManager.getStringValue(RESTORE_ID).isBlank()) {
            val intentFilterRestoreID = IntentFilter(Freshchat.FRESHCHAT_USER_RESTORE_ID_GENERATED)
            getLocalBroadcastManager().registerReceiver(restoreIdReceiver, intentFilterRestoreID)
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

    override fun onTerminate() {
        super.onTerminate()
        getLocalBroadcastManager().unregisterReceiver(restoreIdReceiver)
        getLocalBroadcastManager().unregisterReceiver(unreadCountChangeReceiver)
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
