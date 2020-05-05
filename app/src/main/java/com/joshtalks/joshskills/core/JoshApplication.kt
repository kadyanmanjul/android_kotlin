package com.joshtalks.joshskills.core

import android.content.Context
import android.os.StrictMode
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.multidex.MultiDex
import androidx.work.Configuration
import com.facebook.FacebookSdk
import com.facebook.LoggingBehavior
import com.facebook.stetho.Stetho
import com.joshtalks.joshskills.BuildConfig
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.repository.server.UpdateDeviceRequest
import io.branch.referral.Branch
import io.branch.referral.BranchApp
import io.sentry.core.Sentry
import io.sentry.core.SentryLevel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

var TAG = "JoshSkill"

class JoshApplication : BranchApp(), LifecycleObserver, Configuration.Provider {

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
            Sentry.setLevel(SentryLevel.ERROR)

        }
        AppObjectController.init(this)
    }

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        MultiDex.install(this)
    }

    fun updateDeviceDetail() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (Mentor.getInstance().hasId()) {
                    AppObjectController.signUpNetworkService.updateDeviceDetails(UpdateDeviceRequest())
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }

    fun userActive() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (Mentor.getInstance().hasId()) {
                    AppObjectController.signUpNetworkService.userActive(
                        Mentor.getInstance().getId(),
                        Any()
                    )
                }

            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }


    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun onAppBackgrounded() {
        Timber.tag(TAG).e("************* backgrounded")
        Timber.tag(TAG).e("************* ${isActivityVisible()}")
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun onAppForegrounded() {
        Timber.tag(TAG).e("************* foregrounded")
        Timber.tag(TAG).e("************* ${isActivityVisible()}")

    }

    private fun isActivityVisible(): String {
        return ProcessLifecycleOwner.get().lifecycle.currentState.name
    }

    override fun getWorkManagerConfiguration(): Configuration {
        return Configuration.Builder()
            .setMinimumLoggingLevel(Log.VERBOSE)
            .build()
    }
}
