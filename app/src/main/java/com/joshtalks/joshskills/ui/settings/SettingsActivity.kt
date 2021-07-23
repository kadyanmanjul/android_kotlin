package com.joshtalks.joshskills.ui.settings

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.WebRtcMiddlewareActivity
import com.joshtalks.joshskills.ui.settings.fragments.SettingsFragment
import kotlinx.android.synthetic.main.base_toolbar.iv_back

class SettingsActivity : WebRtcMiddlewareActivity() {

    lateinit var titleView: TextView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        titleView = findViewById(R.id.text_message_title)
        iv_back.visibility = View.VISIBLE
        iv_back.setOnClickListener {
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