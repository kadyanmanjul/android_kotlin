package com.joshtalks.joshskills.ui.settings

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.BaseActivity
import com.joshtalks.joshskills.ui.settings.fragments.SettingsFragment

class SettingsActivity : BaseActivity() {

    lateinit var titleView: TextView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        titleView = findViewById(R.id.text_message_title)
        findViewById<View>(R.id.iv_help).visibility = View.GONE
        findViewById<View>(R.id.iv_back).visibility = View.VISIBLE

        findViewById<View>(R.id.iv_back).setOnClickListener {
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
}