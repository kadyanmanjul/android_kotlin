package com.joshtalks.joshskills.ui.voip.new_arch.ui.views

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.commit
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.base.BaseActivity
import com.joshtalks.joshskills.base.constants.*

import com.joshtalks.joshskills.core.PermissionUtils.isCallingPermissionEnabled
import com.joshtalks.joshskills.databinding.ActivityVoiceCallBinding
import com.joshtalks.joshskills.quizgame.base.GameEventLiveData
import com.joshtalks.joshskills.ui.voip.new_arch.ui.viewmodels.VoiceCallViewModel
import com.joshtalks.joshskills.ui.voip.new_arch.ui.viewmodels.voipLog
import com.joshtalks.joshskills.voip.Utils
import com.joshtalks.joshskills.voip.Utils.Companion.onMultipleBackPress
import com.joshtalks.joshskills.voip.constant.*
import com.joshtalks.joshskills.voip.data.RecordingButtonState
import com.joshtalks.joshskills.voip.data.CallingRemoteService
import com.joshtalks.joshskills.voip.data.local.PrefManager
import com.joshtalks.joshskills.voip.voipanalytics.CallAnalytics
import com.joshtalks.joshskills.voip.voipanalytics.EventName
import kotlinx.coroutines.sync.Mutex

private const val TAG = "VoiceCallActivity"

class VoiceCallActivity : BaseActivity() {
    private val backPressMutex = Mutex(false)
    var recordingPermissionAlert: AlertDialog? = null
    private var isServiceBounded = false
    private val recordingLiveEvent = GameEventLiveData
    private val voiceCallBinding by lazy<ActivityVoiceCallBinding> {
        DataBindingUtil.setContentView(this, R.layout.activity_voice_call)
    }

    val vm by lazy {
        ViewModelProvider(this)[VoiceCallViewModel::class.java]
    }

    private val requestPermissionsLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            if (permissions.all { it.value }) {
                Log.d(TAG, "requestPermissionsLauncher: given")
                vm.boundService(this)
                isServiceBounded = true
            } else {
                Log.d(TAG, "requestPermissionsLauncher: not given")
                Toast.makeText(applicationContext,"Please Allow Permissions to make call",Toast.LENGTH_LONG).show()
                finishAndRemoveTask()
            }
        }

    private fun proceedFurther() {
        vm.source = getSource()
        Log.d(TAG, "getArguments: ${vm.source}")
        when (vm.source) {
            FROM_CALL_BAR -> {}
            FROM_INCOMING_CALL -> {

                val incomingCallId = PrefManager.getIncomingCallId()
                // TODO: Might be wrong
                CallAnalytics.addAnalytics(
                    event = EventName.INCOMING_CALL_ACCEPT,
                    agoraCallId = PrefManager.getAgraCallId().toString(),
                    agoraMentorId = PrefManager.getLocalUserAgoraId().toString()
                )
                vm.callData[INTENT_DATA_INCOMING_CALL_ID] = incomingCallId
                val remoteServiceIntent = Intent(Utils.context, CallingRemoteService::class.java)
                remoteServiceIntent.action = SERVICE_ACTION_INCOMING_CALL_HIDE
                Utils.context?.startService(remoteServiceIntent)
            }
            else -> {
                val topicId = intent?.getStringExtra(INTENT_DATA_TOPIC_ID)
                val courseId = intent?.getStringExtra(INTENT_DATA_COURSE_ID)
                voipLog?.log("Call Data --> $intent")
                vm.callData[INTENT_DATA_COURSE_ID] = courseId ?: "0"
                vm.callData[INTENT_DATA_TOPIC_ID] = topicId ?: "0"
            }
        }
        if (vm.source == FROM_INCOMING_CALL || vm.source == FROM_CALL_BAR) {
            addCallUserFragment()
        } else if (vm.source == FROM_ACTIVITY) {
            addSearchingUserFragment()
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        Log.d(TAG, "onNewIntent: $intent")
    }

    // TODO: Need to refactor
    override fun getArguments() {
      proceedFurther()
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

    override fun onCreated() {}

    private fun getPermissions() {
        requestPermissionsLauncher.launch(
            arrayOf(
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.ACCESS_NETWORK_STATE,
                Manifest.permission.MODIFY_AUDIO_SETTINGS,
            )
        )
    }

    override fun initViewState() {
        event.observe(this) {
            Log.i(TAG, "initViewState: event -> ${it.what}")
            when (it.what) {
                CALL_CONNECTED_EVENT -> replaceCallUserFragment()
                CLOSE_CALL_SCREEN -> finish()
                else -> {
                    if (it.what < 0) {
                        showToast("Error Occurred")
                        finish()
                    }
                }
            }
        }
        recordingLiveEvent.observe(this) {
            Log.i(TAG, "initViewState: event -> ${it.what}")
            when (it.what) {
                SHOW_RECORDING_PERMISSION_DIALOG -> showRecordingPermissionDialog()
                SHOW_RECORDING_REJECTED_DIALOG -> showRecordingRejectedDialog()
                HIDE_RECORDING_PERMISSION_DIALOG -> {
                    hideRecordingPermissionDialog()
                    if (it.obj == true){
                        vm.startAudioVideoRecording(this@VoiceCallActivity.window.decorView)
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

        if(isCallingPermissionEnabled(this)) {
            vm.boundService(this)
            isServiceBounded = true
        }else{
            getPermissions()
        }
    }

    override fun onStop() {
        super.onStop()
        if(isServiceBounded)
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
            if (vm.uiState.recordingButtonState == RecordingButtonState.SENTREQUEST)
                vm.cancelRecording()
            vm.recordingStopButtonClickListener()
            vm.backPress()
        }
    }
}