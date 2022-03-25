package com.joshtalks.joshskills.ui.voip.new_arch.ui.views

import android.os.Bundle
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.commit
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.databinding.ActivityVoiceCallBinding

class VoiceCallActivity : BaseVoiceCallActivity() {

    private val voiceCallBinding by lazy<ActivityVoiceCallBinding> {
        DataBindingUtil.setContentView(this, R.layout.activity_voice_call)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        onCreated()
    }

    override fun initViewBinding() {
        voiceCallBinding.executePendingBindings()
    }

    override fun onCreated() {
        addSearchingUserFragment()
    }

    private fun addSearchingUserFragment() {
        supportFragmentManager.commit {
            add(R.id.voice_call_container, CallFragment(), "SearchingUserFragment")
        }
    }
}