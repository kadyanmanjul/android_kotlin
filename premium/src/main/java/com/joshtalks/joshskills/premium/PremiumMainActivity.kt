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
import com.facebook.FacebookSdk
import com.google.android.play.core.splitcompat.SplitCompat
import com.google.firebase.FirebaseApp
import com.google.firebase.ktx.Firebase
import com.google.firebase.ktx.FirebaseCommonKtxRegistrar
import com.google.firebase.ktx.initialize
import com.google.firebase.remoteconfig.RemoteConfigRegistrar
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.joshtalks.joshskills.PremiumApplication
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
        //AppObjectController.joshApplication = application
        Utils.initUtils(PremiumApplication.premiumApplication)
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
        try {
            if(FacebookSdk.isInitialized().not())
                FacebookSdk.sdkInitialize(applicationContext)
        } catch (e : Exception) {
            e.printStackTrace()
        }
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
        try {
            for (componentPath in PremiumApplication.components) {
                try {
                    packageManager.setComponentEnabledSetting(
                        ComponentName(this, componentPath),
                        PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
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