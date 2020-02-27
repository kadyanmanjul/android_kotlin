package com.joshtalks.joshskills.core

import android.content.Context
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

class JoshApplication : BranchApp() {


    companion object {
        @JvmStatic
        internal var appObjectController: AppObjectController? = null
            private set
    }


    override fun onCreate() {
        super.onCreate()

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
}
