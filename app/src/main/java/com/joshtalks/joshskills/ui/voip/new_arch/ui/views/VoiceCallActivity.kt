package com.joshtalks.joshskills.ui.voip.new_arch.ui.views

import android.os.Bundle
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.commit
import androidx.lifecycle.ViewModelProvider
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.base.BaseActivity
import com.joshtalks.joshskills.base.constants.INTENT_DATA_COURSE_ID
import com.joshtalks.joshskills.base.constants.INTENT_DATA_TOPIC_ID
import com.joshtalks.joshskills.databinding.ActivityVoiceCallBinding
import com.joshtalks.joshskills.ui.voip.new_arch.ui.viewmodels.VoiceCallViewModel
import com.joshtalks.joshskills.voip.constant.CALL_CONNECTED_EVENT
import com.joshtalks.joshskills.voip.constant.CALL_DISCONNECT_REQUEST
import com.joshtalks.joshskills.voip.constant.CALL_INITIATED_EVENT
import com.joshtalks.joshskills.voip.constant.ERROR
import com.joshtalks.joshskills.voip.voipLog

class VoiceCallActivity : BaseActivity() {

    private val voiceCallBinding by lazy<ActivityVoiceCallBinding> {
        DataBindingUtil.setContentView(this, R.layout.activity_voice_call)
    }

    val vm by lazy {
        ViewModelProvider(this)[VoiceCallViewModel::class.java]
    }

    override fun getArguments() {
        val topicId = intent?.getStringExtra(INTENT_DATA_TOPIC_ID)
        val courseId = intent?.getStringExtra(INTENT_DATA_COURSE_ID)
        voipLog?.log("Call Data --> $intent")
        vm.callData[INTENT_DATA_COURSE_ID] = courseId ?: "0"
        vm.callData[INTENT_DATA_TOPIC_ID] = topicId ?: "0"
    }

    override fun initViewBinding() {
        voiceCallBinding.executePendingBindings()
    }

    override fun onCreated() {
        addSearchingUserFragment()
    }

    override fun initViewState() {
        event.observe(this) {
            when(it.what) {
                CALL_INITIATED_EVENT -> addCallUserFragment()
                CALL_DISCONNECT_REQUEST -> finish()
                ERROR -> finish()
            }
        }
    }

    private fun addSearchingUserFragment() {
        supportFragmentManager.commit {
            add(R.id.voice_call_container, SearchingUserFragment(), "SearchingUserFragment")
        }
    }

    private fun addCallUserFragment() {
        supportFragmentManager.commit {
            add(R.id.voice_call_container, CallFragment(), "CallFragment")
        }
    }

    override fun onStart() {
        super.onStart()
        vm.boundService()
        vm.connectCall()
    }

    override fun onStop() {
        super.onStop()
        vm.unboundService()
    }
}