package com.joshtalks.joshskills.ui.launch

import android.os.Bundle
import android.os.Handler
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.CoreJoshActivity
import com.joshtalks.joshskills.core.service.WorkMangerAdmin

class LauncherActivity : CoreJoshActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WorkMangerAdmin.readMessageUpdating()
        setContentView(R.layout.activity_launcher)
        val intent = getIntentForState()
        Handler().postDelayed({
            startActivity(intent)
            this@LauncherActivity.finish()
        }, 2000)

    }
}
