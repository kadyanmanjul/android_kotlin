package com.joshtalks.joshskills.premium.ui.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.TextView
import com.google.android.play.core.splitcompat.SplitCompat
import com.joshtalks.joshskills.premium.R
import com.joshtalks.joshskills.premium.core.CoreJoshActivity
import com.joshtalks.joshskills.premium.core.analytics.MixPanelEvent
import com.joshtalks.joshskills.premium.core.analytics.MixPanelTracker
import com.joshtalks.joshskills.premium.ui.extra.CustomPermissionDialogFragment
import com.joshtalks.joshskills.premium.ui.extra.NOTIFICATION_POPUP
import com.joshtalks.joshskills.premium.ui.settings.fragments.SettingsFragment
import kotlinx.android.synthetic.main.base_toolbar.*

class SettingsActivity : CoreJoshActivity() {

    lateinit var titleView: TextView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        titleView = findViewById(R.id.text_message_title)
        iv_help.visibility = View.GONE
        iv_back.visibility = View.VISIBLE
        iv_back.setOnClickListener {
            MixPanelTracker.publishEvent(MixPanelEvent.BACK).push()
            onBackPressed()
        }

        replaceFragment(R.id.settings_container, SettingsFragment(), SettingsFragment.TAG)
    }

    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(newBase)
        SplitCompat.installActivity(this)
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