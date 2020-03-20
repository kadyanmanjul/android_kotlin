package com.joshtalks.joshskills.core

import android.content.Context
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.multidex.MultiDex
import com.facebook.FacebookSdk
import com.facebook.LoggingBehavior
import com.facebook.stetho.Stetho
import com.joshtalks.joshskills.BuildConfig
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.repository.server.UpdateDeviceRequest
import io.branch.referral.Branch
import io.branch.referral.BranchApp
import io.branch.referral.validators.IntegrationValidator
import io.github.inflationx.calligraphy3.CalligraphyConfig
import io.github.inflationx.calligraphy3.CalligraphyInterceptor
import io.github.inflationx.viewpump.ViewPump
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

class JoshApplication : BranchApp(), LifecycleObserver {
    companion object {
        @JvmStatic
        internal var appObjectController: AppObjectController? = null
            private set
    }


    override fun onCreate() {
        super.onCreate()
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)

        if (BuildConfig.DEBUG) {
            Stetho.initializeWithDefaults(this)
            FacebookSdk.setIsDebugEnabled(true)
            FacebookSdk.addLoggingBehavior(LoggingBehavior.APP_EVENTS)
            Branch.enableDebugMode()
            IntegrationValidator.validate(this)
            Timber.plant(Timber.DebugTree())
        }
        appObjectController = AppObjectController.init(this)

        ViewPump.init(
            ViewPump.builder()
                .addInterceptor(
                    CalligraphyInterceptor(
                        CalligraphyConfig.Builder()
                            .setDefaultFontPath("fonts/OpenSans-Regular.ttf")
                            .setFontAttrId(R.attr.fontPath)
                            .build()
                    )
                )
                .build()
        )
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

    var TAG = "APPPP"

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun onAppBackgrounded() {

        Log.e(TAG, "************* backgrounded")
        Log.e(TAG, "************* ${isActivityVisible()}")
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun onAppForegrounded() {

        Log.e(TAG, "************* foregrounded")
        Log.e(TAG, "************* ${isActivityVisible()}")
        // App in foreground
    }

    private fun isActivityVisible(): String {
        return ProcessLifecycleOwner.get().lifecycle.currentState.name
    }
}
