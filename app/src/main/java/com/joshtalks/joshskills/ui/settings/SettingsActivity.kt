package com.joshtalks.joshskills.ui.settings

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.BaseActivity
import com.joshtalks.joshskills.ui.settings.fragments.SettingsFragment

class SettingsActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        replaceFragment(R.id.settings_container, SettingsFragment(), SettingsFragment.TAG)
    }

    companion object {
        fun getIntent(context: Context): Intent {
            return Intent(context, SettingsActivity::class.java)
        }
    }
}