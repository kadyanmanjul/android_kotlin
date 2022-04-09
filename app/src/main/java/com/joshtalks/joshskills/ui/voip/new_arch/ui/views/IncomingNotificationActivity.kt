package com.joshtalks.joshskills.ui.voip.new_arch.ui.views

import androidx.databinding.DataBindingUtil
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.base.BaseActivity
import com.joshtalks.joshskills.databinding.ActivityIncomingNotificationBinding

class IncomingNotificationActivity : BaseActivity() {

    private val notificationBinding by lazy<ActivityIncomingNotificationBinding> {
        DataBindingUtil.setContentView(this, R.layout.activity_incoming_notification)
    }

    override fun initViewBinding() {
        notificationBinding
    }

    override fun onCreated() {}

    override fun initViewState() {}

}