package com.joshtalks.joshskills.core

import android.content.Intent
import android.view.View
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.ui.help.HelpActivity


abstract class CoreJoshActivity : BaseActivity() {

    override fun onResume() {
        super.onResume()
        try {
            findViewById<View>(R.id.iv_help).setOnClickListener {
                val i = Intent(this, HelpActivity::class.java)
                startActivityForResult(i, HELP_ACTIVITY_REQUEST_CODE)
            }
        } catch (ex: NullPointerException) {
        } catch (ex: Exception) {
        }
    }
}
