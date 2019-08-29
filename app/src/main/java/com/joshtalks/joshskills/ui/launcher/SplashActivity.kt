package com.joshtalks.joshskills.ui.launcher

import android.os.Bundle
import com.joshtalks.joshskills.core.CoreJoshActivity

class SplashActivity : CoreJoshActivity(){

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startActivity(getConfigIntent())
        finish()
    }
}