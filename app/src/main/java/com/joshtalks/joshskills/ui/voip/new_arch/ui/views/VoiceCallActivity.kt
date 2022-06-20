package com.joshtalks.joshskills.ui.voip.new_arch.ui.views

import android.app.AlertDialog
import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.commit
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.base.BaseActivity
import com.joshtalks.joshskills.base.constants.FROM_ACTIVITY
import com.joshtalks.joshskills.base.constants.FROM_CALL_BAR
import com.joshtalks.joshskills.base.constants.FROM_INCOMING_CALL
import com.joshtalks.joshskills.base.constants.INTENT_DATA_COURSE_ID
import com.joshtalks.joshskills.base.constants.INTENT_DATA_INCOMING_CALL_ID
import com.joshtalks.joshskills.base.constants.INTENT_DATA_TOPIC_ID
import com.joshtalks.joshskills.core.PermissionUtils
import com.joshtalks.joshskills.databinding.ActivityVoiceCallBinding
import com.joshtalks.joshskills.ui.voip.new_arch.ui.viewmodels.VoiceCallViewModel
import com.joshtalks.joshskills.ui.voip.new_arch.ui.viewmodels.voipLog
import com.joshtalks.joshskills.voip.Utils.Companion.onMultipleBackPress
import com.joshtalks.joshskills.voip.constant.CALL_CONNECTED_EVENT
import com.joshtalks.joshskills.voip.constant.CLOSE_CALL_SCREEN
import com.joshtalks.joshskills.voip.constant.HIDE_RECORDING_PERMISSION_DIALOG
import com.joshtalks.joshskills.voip.constant.SHOW_RECORDING_PERMISSION_DIALOG
import com.joshtalks.joshskills.voip.constant.SHOW_RECORDING_REJECTED_DIALOG
import com.joshtalks.joshskills.voip.constant.State
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
        Log.d(TAG, "getArguments: ${vm.source}")
        when (vm.source) {
            FROM_CALL_BAR -> {
            }
            FROM_INCOMING_CALL -> {
                val incomingCallId = PrefManager.getIncomingCallId()
                // TODO: Might be wrong
                CallAnalytics.addAnalytics(
                    event = EventName.INCOMING_CALL_ACCEPT,
                    agoraCallId = PrefManager.getAgraCallId().toString(),
                    agoraMentorId = PrefManager.getLocalUserAgoraId().toString()
                )
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
            if (vm.source == FROM_INCOMING_CALL || vm.source == FROM_CALL_BAR) {
                addCallUserFragment()
            } else if (vm.source == FROM_ACTIVITY) {
                addSearchingUserFragment()
            }
        }else{
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
                            if (vm.source == FROM_INCOMING_CALL || vm.source == FROM_CALL_BAR) {
                                addCallUserFragment()
                            } else if (vm.source == FROM_ACTIVITY) {
                                addSearchingUserFragment()
                            }
                            return
                        } else {
                            finish()
                        }
                    }
                }
                override fun onPermissionRationaleShouldBeShown(
                    permissions: MutableList<PermissionRequest>?,
                    token: PermissionToken?
                ) {
                    token?.continuePermissionRequest()
                }
            }
        )
    }


    override fun initViewState() {
        event.observe(this) {
            Log.i(TAG, "initViewState: event -> ${it.what}")
            when (it.what) {
                CALL_CONNECTED_EVENT -> replaceCallUserFragment()
                SHOW_RECORDING_PERMISSION_DIALOG -> showRecordingPermissionDialog()
                SHOW_RECORDING_REJECTED_DIALOG -> showRecordingRejectedDialog()
                HIDE_RECORDING_PERMISSION_DIALOG -> {
                    hideRecordingPermissionDialog()
                    if (it.obj == true){
                        vm.startAudioVideoRecording(this@VoiceCallActivity.window.decorView)
                    }
                }
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

    private fun showRecordingRejectedDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Recording request rejected")
            .setMessage("User declined your request to start recording")
            .setPositiveButton("Dismiss") { dialog, _ ->
                dialog.dismiss()
            }
            .show()

    }

    private fun showRecordingPermissionDialog() {
        recordingPermissionAlert = AlertDialog.Builder(this).apply {
            setView(
                LayoutInflater.from(this@VoiceCallActivity)
                    .inflate(R.layout.dialog_record_call, null)
            )
            setPositiveButton("ACCEPT") { dialog, _ ->
                vm.recordingStartedUIChanges()
                vm.acceptCallRecording(this@VoiceCallActivity.window.decorView)
                dialog.dismiss()
            }
            setNegativeButton("DECLINE") { dialog, which ->
                vm.rejectCallRecording()
                dialog.dismiss()
            }
            setOnCancelListener {
                    vm.rejectCallRecording()
            }
        }.create()
        recordingPermissionAlert?.setCanceledOnTouchOutside(false)
        recordingPermissionAlert?.show()
    }

    private fun recordingAlreadyStartedByOtherUserDialog() {
        Toast.makeText(this, "Recording was already started by the other person", Toast.LENGTH_SHORT).show()
    }

    private fun hideRecordingPermissionDialog() {
        Log.i(TAG, "hideRecordingPermissionDialog: ")
        recordingPermissionAlert?.dismiss()
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
            vm.recordingStopButtonClickListener()
            vm.backPress()
        }
    }
}