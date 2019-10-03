package com.joshtalks.joshskills.core

import android.content.Intent
import android.os.Bundle
import com.joshtalks.joshskills.core.service.HAS_NOTIFICATION
import com.joshtalks.joshskills.core.service.NOTIFICATION_ID
import com.joshtalks.joshskills.repository.service.EngagementNetworkHelper

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
