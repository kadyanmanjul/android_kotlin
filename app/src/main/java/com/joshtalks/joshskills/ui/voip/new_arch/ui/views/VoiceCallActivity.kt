package com.joshtalks.joshskills.ui.voip.new_arch.ui.views

import android.app.AlertDialog
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
import com.joshtalks.joshskills.base.constants.INTENT_DATA_COURSE_ID
import com.joshtalks.joshskills.base.constants.INTENT_DATA_INCOMING_CALL_ID
import com.joshtalks.joshskills.base.constants.INTENT_DATA_TOPIC_ID
import com.joshtalks.joshskills.core.PermissionUtils
import com.joshtalks.joshskills.base.constants.*
import com.joshtalks.joshskills.databinding.ActivityVoiceCallBinding
import com.joshtalks.joshskills.ui.voip.new_arch.ui.viewmodels.VoiceCallViewModel
import com.joshtalks.joshskills.ui.voip.new_arch.ui.viewmodels.voipLog
import com.joshtalks.joshskills.voip.Utils.Companion.onMultipleBackPress
import com.joshtalks.joshskills.voip.constant.*
import com.joshtalks.joshskills.voip.data.local.PrefManager
import com.joshtalks.joshskills.voip.voipanalytics.CallAnalytics
import com.joshtalks.joshskills.voip.voipanalytics.EventName
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import kotlinx.coroutines.sync.Mutex

private const val TAG = "VoiceCallActivity"

class VoiceCallActivity : BaseActivity() {
    private val backPressMutex = Mutex(false)
    var recordingPermissionAlert: AlertDialog? = null
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
        vm.source = getSource()
        vm.callType = Category.values()[intent.getIntExtra(INTENT_DATA_CALL_CATEGORY, PrefManager.getCallCategory().ordinal)]

        Log.d(TAG, "getArguments: ${vm.source}  ${vm.callType}")

        when (vm.source) {
            FROM_CALL_BAR -> {}
            FROM_INCOMING_CALL -> {
                val incomingCallId = PrefManager.getIncomingCallId()
                CallAnalytics.addAnalytics(
                    event = EventName.INCOMING_CALL_ACCEPT,
                    agoraCallId = PrefManager.getAgraCallId().toString(),
                    agoraMentorId = PrefManager.getLocalUserAgoraId().toString()
                )
                vm.callData[INTENT_DATA_INCOMING_CALL_ID] = incomingCallId
            }
            else -> {
                setCallData()
            }
        }
    }

    private fun setCallData() {
        when (vm.callType) {
            Category.PEER_TO_PEER -> {
                val topicId = intent?.getStringExtra(INTENT_DATA_TOPIC_ID)
                val courseId = intent?.getStringExtra(INTENT_DATA_COURSE_ID)
                vm.callData[INTENT_DATA_COURSE_ID] = courseId ?: "0"
                vm.callData[INTENT_DATA_TOPIC_ID] = topicId ?: "0"
            }
            Category.FPP ->{
                val mentorId = intent?.getStringExtra(INTENT_DATA_FPP_MENTOR_ID)
                vm.callData[INTENT_DATA_FPP_MENTOR_ID] = mentorId ?: "0"
            }
            Category.GROUP -> {}
        }
    }

    private fun getSource(): String {
        val topicId = intent?.getStringExtra(INTENT_DATA_TOPIC_ID)
        val mentorId = intent?.getStringExtra(INTENT_DATA_FPP_MENTOR_ID)
        val shouldOpenCallFragment = (topicId == null && mentorId == null)
        return if (shouldOpenCallFragment && PrefManager.getVoipState() == State.IDLE)
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
        Log.d(TAG, "onCreated: ${vm.source}")
        if (PermissionUtils.isCallingPermissionEnabled(this)) {
            startCallingScreen()
        } else {
            getPermission()
        }
    }

    private fun getPermission() {
        PermissionUtils.callingFeaturePermission(
            this,
            object : MultiplePermissionsListener {
                override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                    report?.areAllPermissionsGranted()?.let { flag ->
                        if (report.isAnyPermissionPermanentlyDenied) {
                            PermissionUtils.callingPermissionPermanentlyDeniedDialog(
                                this@VoiceCallActivity,
                                message = R.string.call_start_permission_message
                            )
                            return
                        }
                        if (flag) {
                            startCallingScreen()
                            return
                        } else {
                            finish()
                        }
                    }
                }

                override fun onPermissionRationaleShouldBeShown(
                    permissions: MutableList<PermissionRequest>?,
                    token: PermissionToken?,
                ) {
                    token?.continuePermissionRequest()
                }
            }
        )
    }

    private fun startCallingScreen() {
        when (vm.callType) {
            Category.PEER_TO_PEER -> {
                openFragment { addCallUserFragment() }
            }
            Category.FPP -> {
                addFppCallFragment()
            }
            Category.GROUP -> {
//                openFragment { addGroupCallFragment() }
            }
        }
    }


    private fun openFragment(fragment: () -> Unit) {
        if (vm.source == FROM_INCOMING_CALL || vm.source == FROM_CALL_BAR) {
            fragment.invoke()
        } else if (vm.source == FROM_ACTIVITY) {
            addSearchingUserFragment()
        }
    }


    override fun initViewState() {
        event.observe(this) {
            when (it.what) {
                CALL_INITIATED_EVENT -> replaceCallUserFragment()
                CLOSE_CALL_SCREEN -> finish()
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

    private fun addFppCallFragment() {
        supportFragmentManager.commit {
            add(R.id.voice_call_container, FppCallFragment(), "FppCallFragment")
        }
    }


    private fun replaceCallUserFragment() {
        supportFragmentManager.commit {
            replace(R.id.voice_call_container, CallFragment(), "CallFragment")
        }
    }

    override fun onStart() {
        super.onStart()
        vm.boundService(this)
    }

    override fun onStop() {
        super.onStop()
        vm.unboundService(this)
    }

    override fun onBackPressed() {
        if (PrefManager.getVoipState() == State.IDLE || PrefManager.getVoipState() == State.SEARCHING || PrefManager.getVoipState() == State.JOINING || PrefManager.getVoipState() == State.JOINED)
            backPressMutex.onMultipleBackPress {
                super.onBackPressed()
                vm.backPress()
            }
        else {
            super.onBackPressed()
            vm.backPress()
        }
    }
}