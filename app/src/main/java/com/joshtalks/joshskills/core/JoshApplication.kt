package com.joshtalks.joshskills.core

import android.app.Application
import android.util.Log
import androidx.work.Configuration
import com.crashlytics.android.Crashlytics
import com.crashlytics.android.core.CrashlyticsCore
import com.facebook.stetho.Stetho
import com.joshtalks.joshskills.BuildConfig
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.repository.server.CoreMeta
import io.fabric.sdk.android.Fabric
import io.github.inflationx.calligraphy3.CalligraphyConfig
import io.github.inflationx.calligraphy3.CalligraphyInterceptor
import io.github.inflationx.viewpump.ViewPump
import kotlinx.coroutines.*


class JoshApplication : Application(){


    companion object {
        @JvmStatic
        internal var appObjectController: AppObjectController? = null
            private set
    }


    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG) {
            Stetho.initializeWithDefaults(this);

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
        Fabric.with(
            this, Crashlytics.Builder()
                .core(
                    CrashlyticsCore.Builder()
                        .disabled(BuildConfig.DEBUG)
                        .build()
                )
                .build()
        )
    }


    fun fetchCoreMeta() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val meta: CoreMeta = AppObjectController.signUpNetworkService.getCoreMeta().await()

            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }
}
