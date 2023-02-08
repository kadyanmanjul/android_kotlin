package com.joshtalks.joshskills.premium

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import androidx.lifecycle.ProcessLifecycleInitializer
import androidx.startup.AppInitializer
import androidx.work.WorkManagerInitializer
import com.google.android.play.core.splitcompat.SplitCompat
import com.google.firebase.FirebaseApp
import com.google.firebase.ktx.Firebase
import com.google.firebase.ktx.FirebaseCommonKtxRegistrar
import com.google.firebase.ktx.initialize
import com.google.firebase.remoteconfig.RemoteConfigRegistrar
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.joshtalks.joshskills.premium.core.AppObjectController
import com.joshtalks.joshskills.premium.ui.launch.LauncherActivity
import com.joshtalks.joshskills.voip.Utils
import io.branch.referral.Branch
import com.joshtalks.joshskills.premium.BuildConfig

class PremiumMainActivity : AppCompatActivity() {
    val btnEnterPremium by lazy {
        findViewById<Button>(R.id.btn_enter_premium)
    }

    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(newBase)
        enableProvider()
        SplitCompat.installActivity(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_premium_main)
        AppObjectController.joshApplication = application
        Utils.initUtils(application)
//        AppInitializer.getInstance(applicationContext).initializeComponent(ProcessLifecycleInitializer::class.java)
        if (BuildConfig.DEBUG) {
            Branch.enableTestMode()
            Branch.enableLogging()
        }
        // TODO: Need to be removed
        Branch.getAutoInstance(this)
        //turnOnStrictMode()
        RemoteConfigRegistrar()
        AppObjectController.init()
        AppObjectController.initFirebaseRemoteConfig()
        AppObjectController.configureCrashlytics()
        AppInitializer.getInstance(applicationContext).initializeComponent(WorkManagerInitializer::class.java)

        btnEnterPremium.setOnClickListener {
            try {
                startActivity(Intent(this, LauncherActivity::class.java))
            }catch (e : Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun enableProvider() {
        for (componentPath in components) {
            packageManager.setComponentEnabledSetting(
                ComponentName(this, componentPath),
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP
            )
        }
    }

    val components = listOf(
        "com.google.firebase.perf.provider.FirebasePerfProvider",
        "com.userexperior.provider.UeContentProvider",
        //"androidx.startup.InitializationProvider",
        //"com.joshtalks.joshskills.GenericFileProvider",
        "androidx.core.content.FileProvider",
        "com.joshtalks.joshskills.premium.core.contentprovider.JoshContentProvider",
        "com.freshchat.consumer.sdk.provider.FreshchatInitProvider",
        "com.github.dhaval2404.imagepicker.ImagePickerFileProvider",
        "com.facebook.internal.FacebookInitProvider",
        "com.squareup.picasso.PicassoProvider",
        //"com.google.firebase.components.ComponentDiscoveryService"
    )
}