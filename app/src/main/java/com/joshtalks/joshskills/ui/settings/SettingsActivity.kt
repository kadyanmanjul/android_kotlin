package com.joshtalks.joshskills.ui.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageView
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.CoreJoshActivity
import com.joshtalks.joshskills.core.analytics.MixPanelEvent
import com.joshtalks.joshskills.core.analytics.MixPanelTracker
import com.joshtalks.joshskills.ui.extra.CustomPermissionDialogFragment
import com.joshtalks.joshskills.ui.extra.NOTIFICATION_POPUP
import com.joshtalks.joshskills.ui.settings.fragments.SettingsFragment

class SettingsActivity : CoreJoshActivity() {

    lateinit var titleView: TextView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        titleView = findViewById(R.id.text_message_title)
        findViewById<AppCompatImageView>(R.id.iv_help).visibility = View.GONE
        findViewById<AppCompatImageView>(R.id.iv_back).visibility = View.VISIBLE
        findViewById<AppCompatImageView>(R.id.iv_back).setOnClickListener {
            MixPanelTracker.publishEvent(MixPanelEvent.BACK).push()
            onBackPressed()
        }

        replaceFragment(R.id.settings_container, SettingsFragment(), SettingsFragment.TAG)
    }

    companion object {
        fun getIntent(context: Context): Intent {
            return Intent(context, SettingsActivity::class.java)
        }
    }

    fun setTitle(title: String) {
        titleView.text = title
    }

    fun openAppNotificationSettings() {
        if (isNotificationEnabled()) {
            val oemIntent = Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse("package:$packageName")
            )
            oemIntent.addCategory(Intent.CATEGORY_DEFAULT)
            oemIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK

            CustomPermissionDialogFragment.showCustomPermissionDialog(
                oemIntent,
                supportFragmentManager,
                NOTIFICATION_POPUP
            )
        }
    }
}