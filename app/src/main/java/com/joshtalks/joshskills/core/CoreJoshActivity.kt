package com.joshtalks.joshskills.core

import android.os.Build
import android.os.Bundle
import androidx.core.content.ContextCompat
import com.joshtalks.joshskills.R

abstract class CoreJoshActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        routeApplicationState()
    }


    private fun routeApplicationState() {
        val intent = getIntentForState()
        if (intent != null) {
            startActivity(intent)
            finish()
        }
    }


}
