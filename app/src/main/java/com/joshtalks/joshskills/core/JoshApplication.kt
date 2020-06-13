package com.joshtalks.joshskills.core

import android.content.Context
import android.os.StrictMode
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.multidex.MultiDex
import com.facebook.FacebookSdk
import com.facebook.LoggingBehavior
import com.facebook.appevents.AppEventsConstants
import com.facebook.stetho.Stetho
import com.joshtalks.joshskills.BuildConfig
import io.branch.referral.Branch
import io.branch.referral.BranchApp
import io.github.inflationx.viewpump.ViewPumpContextWrapper
import io.sentry.core.Sentry
import io.sentry.core.SentryLevel
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
            Sentry.setLevel(SentryLevel.ERROR)

        }
        AppObjectController.init(this)
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
        AppObjectController.facebookEventLogger.logEvent(AppEventsConstants.EVENT_NAME_DEACTIVATED_APP)

    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun onAppForegrounded() {
        Timber.tag(TAG).e("************* foregrounded")
        Timber.tag(TAG).e("************* ${isActivityVisible()}")
        AppObjectController.facebookEventLogger.logEvent(AppEventsConstants.EVENT_NAME_ACTIVATED_APP)
        isAppVisible = true
    }

    private fun isActivityVisible(): String {
        return ProcessLifecycleOwner.get().lifecycle.currentState.name
    }
/*
    override fun getWorkManagerConfiguration(): Configuration {
        return Configuration.Builder()
            .setMinimumLoggingLevel(Log.VERBOSE)
            .build()
    }*/
}
