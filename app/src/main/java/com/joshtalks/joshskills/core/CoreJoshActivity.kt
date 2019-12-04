package com.joshtalks.joshskills.core

import android.os.Bundle

abstract class CoreJoshActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        routeApplicationState()
        super.onCreate(savedInstanceState)

    }


    private fun routeApplicationState() {
        val intent = getIntentForState()
        if (intent != null) {
            startActivity(intent)
            finish()
        }
    }

}
