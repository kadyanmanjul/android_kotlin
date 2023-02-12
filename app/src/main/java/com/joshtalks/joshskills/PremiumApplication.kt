package com.joshtalks.joshskills

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
//import androidx.multidex.MultiDex
import com.google.android.play.core.splitcompat.SplitCompat
import com.google.android.play.core.splitcompat.SplitCompatApplication


class PremiumApplication : SplitCompatApplication() {

    companion object {
        lateinit var premiumApplication: PremiumApplication

        val components = emptyList<String>()
    }

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        SplitCompat.install(this)
        //MultiDex.install(this)
        disableProvider()
    }

    override fun onCreate() {
        super.onCreate()
        premiumApplication = this
    }

    private fun disableProvider() {
        try {
            for (componentPath in components) {
                try {
                    packageManager.setComponentEnabledSetting(
                        ComponentName(this, componentPath),
                        PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                        PackageManager.DONT_KILL_APP
                    )
                } catch (e : Exception) {
                    e.printStackTrace()
                }
            }
        } catch (e : Exception) {
            e.printStackTrace()
        }
    }
}