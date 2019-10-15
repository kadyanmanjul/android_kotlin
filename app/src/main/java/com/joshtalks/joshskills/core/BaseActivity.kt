package com.joshtalks.joshskills.core

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.DisplayMetrics
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.service.HAS_NOTIFICATION
import com.joshtalks.joshskills.core.service.NOTIFICATION_ID
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.repository.local.model.User
import com.joshtalks.joshskills.repository.service.EngagementNetworkHelper
import com.joshtalks.joshskills.ui.inbox.InboxActivity
import com.joshtalks.joshskills.ui.location.SelectLocationActivity
import com.joshtalks.joshskills.ui.profile.CropImageActivity
import com.joshtalks.joshskills.ui.profile.ProfileActivity
import com.joshtalks.joshskills.ui.profile.SOURCE_IMAGE
import com.joshtalks.joshskills.ui.sign_up_old.OnBoardActivity
import io.github.inflationx.viewpump.ViewPumpContextWrapper


abstract class BaseActivity : AppCompatActivity() {

    protected val TAG = javaClass.canonicalName


    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(newBase?.let { ViewPumpContextWrapper.wrap(it) })
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.statusBarColor =
                ContextCompat.getColor(applicationContext, R.color.status_bar_color)
        }
        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        AppObjectController.screenHeight = displayMetrics.heightPixels
        AppObjectController.screenWidth = displayMetrics.widthPixels
        getConfig()


    }

    fun getIntentForState(): Intent? {

        var intent: Intent? = null
        if (User.getInstance().token == null) {
            intent = Intent(this, OnBoardActivity::class.java)
        } else if (User.getInstance().dateOfBirth == null || User.getInstance().dateOfBirth.isNullOrEmpty()) {
            intent = Intent(this, ProfileActivity::class.java)
        }
        return intent?.apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    fun getCroppingActivity(filePath: String): Intent {
        return Intent(this, CropImageActivity::class.java).apply {
            putExtra(SOURCE_IMAGE, filePath)
        }
    }

    fun getInboxActivityIntent(): Intent {
        return Intent(this, InboxActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    protected fun openSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        val uri = Uri.fromParts("package", packageName, null);
        intent.data = uri;
        startActivityForResult(intent, 101);
    }

    protected fun processIntent(intent: Intent?) {
        intent?.hasExtra(HAS_NOTIFICATION)?.let {
            if (it) intent.hasExtra(NOTIFICATION_ID).let {
                if (it) {
                    EngagementNetworkHelper.clickNotification(intent.run {
                        getStringExtra(
                            NOTIFICATION_ID
                        )
                    })
                }
            }
        }
    }

    private fun getConfig() {
        val configSettings = FirebaseRemoteConfigSettings.Builder()
            .setMinimumFetchIntervalInSeconds(10 * 3600)
            .build()
        AppObjectController.firebaseRemoteConfig.setConfigSettingsAsync(configSettings)
        AppObjectController.firebaseRemoteConfig.setDefaultsAsync(R.xml.remote_config_defaults)
        AppObjectController.firebaseRemoteConfig.fetchAndActivate()

    }
}