package com.joshtalks.joshskills.ui.voip.new_arch.ui.views

import android.content.Intent
import android.util.Log
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.commit
import androidx.lifecycle.ViewModelProvider
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.base.BaseActivity
import com.joshtalks.joshskills.base.constants.FROM_ACTIVITY
import com.joshtalks.joshskills.base.constants.FROM_CALL_BAR
import com.joshtalks.joshskills.base.constants.FROM_INCOMING_CALL
import com.joshtalks.joshskills.base.constants.INCOMING_CALL_ID
import com.joshtalks.joshskills.base.constants.INTENT_DATA_COURSE_ID
import com.joshtalks.joshskills.base.constants.INTENT_DATA_INCOMING_CALL_ID
import com.joshtalks.joshskills.base.constants.INTENT_DATA_TOPIC_ID
import com.joshtalks.joshskills.base.constants.STARTING_POINT
import com.joshtalks.joshskills.databinding.ActivityVoiceCallBinding
import com.joshtalks.joshskills.ui.group.data.GroupChatPagingSource
import com.joshtalks.joshskills.ui.voip.new_arch.ui.callbar.VoipPref
import com.joshtalks.joshskills.ui.voip.new_arch.ui.viewmodels.VoiceCallViewModel
import com.joshtalks.joshskills.voip.constant.CALL_CONNECTED_EVENT
import com.joshtalks.joshskills.voip.constant.CALL_DISCONNECT_REQUEST
import com.joshtalks.joshskills.voip.constant.CALL_INITIATED_EVENT
import com.joshtalks.joshskills.voip.constant.CONNECTED
import com.joshtalks.joshskills.voip.constant.IDLE
import com.joshtalks.joshskills.voip.voipLog

private const val TAG = "VoiceCallActivity"

class VoiceCallActivity : BaseActivity() {
    //private val ERROR_RANGE = -1 downTo Int.MIN_VALUE
    private lateinit var source: String

    private val voiceCallBinding by lazy<ActivityVoiceCallBinding> {
        DataBindingUtil.setContentView(this, R.layout.activity_voice_call)
    }

    val vm by lazy {
        ViewModelProvider(this)[VoiceCallViewModel::class.java]
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        Log.d(TAG, "onNewIntent: $intent")
    }

    // TODO: Need to refactor
    override fun getArguments() {
        source = getSource()
        Log.d(TAG, "getArguments: $source")
        when (source) {
            FROM_CALL_BAR -> {}
            FROM_INCOMING_CALL -> {
                val incomingCallId = VoipPref.getIncomingCallId()
                vm.callData[INTENT_DATA_INCOMING_CALL_ID] = incomingCallId
            }
            else -> {
                val topicId = intent?.getStringExtra(INTENT_DATA_TOPIC_ID)
                val courseId = intent?.getStringExtra(INTENT_DATA_COURSE_ID)
                voipLog?.log("Call Data --> $intent")
                vm.callData[INTENT_DATA_COURSE_ID] = courseId ?: "0"
                vm.callData[INTENT_DATA_TOPIC_ID] = topicId ?: "0"
            }
        }
    }

    private fun getSource(): String {
        val topicId = intent?.getStringExtra(INTENT_DATA_TOPIC_ID)
        val shouldOpenCallFragment = (topicId == null)
        return if (shouldOpenCallFragment && VoipPref.getVoipState() == IDLE)
            FROM_INCOMING_CALL
        else if (shouldOpenCallFragment)
            FROM_CALL_BAR
        else
            FROM_ACTIVITY
    }

    override fun initViewBinding() {
        voiceCallBinding.executePendingBindings()
    }

    override fun onCreated() {
        Log.d(TAG, "onCreated: $source")
        if (source == FROM_INCOMING_CALL || source == FROM_CALL_BAR) {
            addCallUserFragment()
        } else {
            addSearchingUserFragment()
        }
    }

    override fun initViewState() {
        event.observe(this) {
            when (it.what) {
                CALL_CONNECTED_EVENT -> replaceCallUserFragment()
                CALL_DISCONNECT_REQUEST -> finish()
                else -> {
                    if (it.what < 0) {
                        showToast("Error Occurred")
                        finish()
                    }
                }
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

    private fun replaceCallUserFragment() {
        supportFragmentManager.commit {
            replace(R.id.voice_call_container, CallFragment(), "CallFragment")
        }
    }

    override fun onStart() {
        super.onStart()
        vm.boundService()
    }

    override fun onStop() {
        super.onStop()
        vm.unboundService()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        if(VoipPref.getVoipState() != CONNECTED)
            vm.disconnect()
    }
}