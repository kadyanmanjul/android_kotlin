package com.joshtalks.joshskills

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
//import androidx.multidex.MultiDex
import com.google.android.play.core.splitcompat.SplitCompat
import com.google.android.play.core.splitcompat.SplitCompatApplication


class PremiumApplication : SplitCompatApplication() {
    val components = listOf(
        "com.google.firebase.perf.provider.FirebasePerfProvider",
        "com.userexperior.provider.UeContentProvider",
        //"androidx.startup.InitializationProvider",
        //"com.joshtalks.joshskills.premium.repository.service.GenericFileProvider",
        "androidx.core.content.FileProvider",
        "com.joshtalks.joshskills.premium.core.contentprovider.JoshContentProvider",
        "com.freshchat.consumer.sdk.provider.FreshchatInitProvider",
        "com.github.dhaval2404.imagepicker.ImagePickerFileProvider",
        "com.facebook.internal.FacebookInitProvider",
        "com.squareup.picasso.PicassoProvider",
        //"com.google.firebase.components.ComponentDiscoveryService"
    )
    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        SplitCompat.install(this)
        //MultiDex.install(this)
        disableProvider()
    }

    private fun disableProvider() {
        for (componentPath in components) {
            packageManager.setComponentEnabledSetting(
                ComponentName(this, componentPath),
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP
            )
        }
    }
}