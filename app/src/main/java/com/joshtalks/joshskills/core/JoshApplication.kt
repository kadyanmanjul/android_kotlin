package com.joshtalks.joshskills.core

import android.content.Context
import android.os.Environment
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
import com.joshtalks.filelogger.FL
import com.joshtalks.filelogger.FLConfig
import com.joshtalks.filelogger.FLConst
import com.joshtalks.joshskills.BuildConfig
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.repository.server.UpdateDeviceRequest
import io.branch.referral.Branch
import io.branch.referral.BranchApp
import io.github.inflationx.calligraphy3.CalligraphyConfig
import io.github.inflationx.calligraphy3.CalligraphyInterceptor
import io.github.inflationx.viewpump.ViewPump
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File

class JoshApplication : BranchApp(), LifecycleObserver, Configuration.Provider {
    companion object {
        @JvmStatic
        internal var appObjectController: AppObjectController? = null
            private set
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
            Branch.enableDebugMode()
            Branch.enableSimulateInstalls()
            // IntegrationValidator.validate(this)
            Timber.plant(Timber.DebugTree())

            FL.init(
                FLConfig.Builder(this)
                    .defaultTag("Default Tag")   // customise default tag
                    .logToFile(true)   // enable logging to file
                    .minLevel(FLConst.Level.V)
                    .dir(File(Environment.getExternalStorageDirectory(), "file_logger_demo"))
                    .retentionPolicy(FLConst.RetentionPolicy.FILE_COUNT)
                    .maxFileCount(FLConst.DEFAULT_MAX_FILE_COUNT)    // customise how many log files to keep if retention by file count
                    .maxTotalSize(FLConst.DEFAULT_MAX_TOTAL_SIZE)    // customise how much space log files can occupy if retention by total size
                    .build()

            )
            FL.setEnabled(true)
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

    override fun getWorkManagerConfiguration(): Configuration {
        return Configuration.Builder()
            .setMinimumLoggingLevel(Log.VERBOSE)
            .build()
    }
}
