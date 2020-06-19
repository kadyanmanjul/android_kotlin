package com.joshtalks.joshskills.core

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.StrictMode
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.multidex.MultiDex
import com.facebook.FacebookSdk
import com.facebook.LoggingBehavior
import com.facebook.stetho.Stetho
import com.freshchat.consumer.sdk.Freshchat
import com.joshtalks.joshskills.BuildConfig
import io.branch.referral.Branch
import io.branch.referral.BranchApp
import io.github.inflationx.viewpump.ViewPumpContextWrapper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

var TAG = "JoshSkill"

class JoshApplication : BranchApp(), LifecycleObserver/*, Configuration.Provider*/ {
    companion object {
        @JvmStatic
        var isAppVisible = false
    }

    override fun onCreate() {
        super.onCreate()
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)

        if (BuildConfig.DEBUG) {
            StrictMode.setVmPolicy(
                StrictMode.VmPolicy.Builder().detectActivityLeaks().detectLeakedClosableObjects()
                    .penaltyLog()
                    .build()
            )
            Stetho.initializeWithDefaults(this)
            FacebookSdk.setIsDebugEnabled(true)
            FacebookSdk.addLoggingBehavior(LoggingBehavior.APP_EVENTS)
            Branch.enableLogging()
            Branch.enableDebugMode()
            Branch.enableSimulateInstalls()
            Branch.enableTestMode()
            Timber.plant(Timber.DebugTree())
        }
        AppObjectController.init(this)
        registerBroadcastReceiver()
    }

    private fun registerBroadcastReceiver() {
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
                        val restoreId = AppObjectController.freshChat.user.restoreId
                        if (restoreId.isNullOrBlank().not()) {
                            PrefManager.put(RESTORE_ID, restoreId)
                            val requestMap = mutableMapOf<String, String?>()
                            requestMap["restore_id"] = restoreId
                            val responseObj =
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

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        MultiDex.install(this)
        base?.let { ViewPumpContextWrapper.wrap(it) }
    }


    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun onAppBackgrounded() {
        Timber.tag(TAG).e("************* backgrounded")
        Timber.tag(TAG).e("************* ${isActivityVisible()}")
        isAppVisible = false
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun onAppForegrounded() {
        Timber.tag(TAG).e("************* foregrounded")
        Timber.tag(TAG).e("************* ${isActivityVisible()}")
        isAppVisible = true
    }

    private fun isActivityVisible(): String {
        return ProcessLifecycleOwner.get().lifecycle.currentState.name
    }
}
