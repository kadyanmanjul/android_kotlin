package com.joshtalks.joshskills.premium.ui.voip.new_arch.ui.views

import android.Manifest
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.commit
import androidx.lifecycle.ViewModelProvider
import com.google.android.play.core.splitcompat.SplitCompat
import com.joshtalks.joshskills.premium.R
import com.joshtalks.joshskills.premium.base.BaseActivity
import com.joshtalks.joshskills.base.constants.*
import com.joshtalks.joshskills.premium.core.*
import com.joshtalks.joshskills.premium.core.PermissionUtils.callingPermissionPermanentlyDeniedDialog
import com.joshtalks.joshskills.premium.core.PermissionUtils.isCallingPermissionEnabled
import com.joshtalks.joshskills.premium.databinding.ActivityVoiceCallBinding
import com.joshtalks.joshskills.premium.ui.voip.new_arch.ui.viewmodels.VoiceCallViewModel
import com.joshtalks.joshskills.premium.ui.voip.new_arch.ui.viewmodels.voipLog
import com.joshtalks.joshskills.premium.calling.Utils
import com.joshtalks.joshskills.premium.calling.Utils.Companion.onMultipleBackPress
import com.joshtalks.joshskills.premium.calling.constant.*
import com.joshtalks.joshskills.premium.calling.data.CallingRemoteService
import com.joshtalks.joshskills.premium.calling.data.local.PrefManager
import com.joshtalks.joshskills.premium.calling.voipanalytics.CallAnalytics
import com.joshtalks.joshskills.premium.calling.voipanalytics.EventName
import kotlinx.coroutines.sync.Mutex
import java.io.File
import com.joshtalks.joshskills.premium.core.PrefManager as CorePrefManager

private const val TAG = "VoiceCallActivity"

class VoiceCallActivity : BaseActivity() {

    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(newBase)
        SplitCompat.installActivity(this)
    }

    private val backPressMutex = Mutex(false)
    private var isServiceBounded = false
    var file: File? = null
    var currentFileName: String? = null
    lateinit var dialog: AlertDialog

    companion object {
        var showDialog = true
        var recordData: Intent? = null
        var recordResultCode: Int? = null
    }

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
                vm.isPermissionGranted.set(true)
            } else {
                Log.d(TAG, "requestPermissionsLauncher: not given")
                Toast.makeText(
                    applicationContext,
                    "Please Allow Permissions to make call",
                    Toast.LENGTH_LONG
                ).show()
                finishAndRemoveTask()
            }
        }


    private fun getPermissions() {
        var shouldLaunchPermission = true
//        val permissions = arrayOf(
//            Manifest.permission.RECORD_AUDIO,
//            Manifest.permission.READ_PHONE_STATE,
//            Manifest.permission.ACCESS_NETWORK_STATE,
//            Manifest.permission.MODIFY_AUDIO_SETTINGS,
//        )

//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//            for (permission in permissions) {
//                if (shouldShowRequestPermissionRationale(permission)) {
//                    Log.d(TAG, "getPermissions: $permission")
//                    shouldLaunchPermission = false
//                    break
//                }
//            }
//        }

        if (shouldLaunchPermission) {
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
                requestPermissionsLauncher.launch(
                    arrayOf(
                        Manifest.permission.RECORD_AUDIO,
                        Manifest.permission.READ_PHONE_STATE,
                        Manifest.permission.ACCESS_NETWORK_STATE,
                        Manifest.permission.MODIFY_AUDIO_SETTINGS
                    )
                )
            } else if (Build.VERSION.SDK_INT > Build.VERSION_CODES.Q && Build.VERSION.SDK_INT <= Build.VERSION_CODES.R) {
                requestPermissionsLauncher.launch(
                    arrayOf(
                        Manifest.permission.RECORD_AUDIO,
                        Manifest.permission.READ_PHONE_STATE,
                        Manifest.permission.ACCESS_NETWORK_STATE,
                        Manifest.permission.MODIFY_AUDIO_SETTINGS,

                        )
                )
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                requestPermissionsLauncher.launch(
                    arrayOf(
                        Manifest.permission.RECORD_AUDIO,
                        Manifest.permission.READ_PHONE_STATE,
                        Manifest.permission.ACCESS_NETWORK_STATE,
                        Manifest.permission.MODIFY_AUDIO_SETTINGS,
                        Manifest.permission.BLUETOOTH_CONNECT
                    )
                )
            }
        } else {
            callingPermissionPermanentlyDeniedDialog(
                this,
                message = R.string.call_start_permission_message
            )
        }
    }

    override fun getArguments() {
        vm.source = getSource()
        vm.callType = com.joshtalks.joshskills.premium.calling.constant.Category.values()[intent.getIntExtra(
            INTENT_DATA_CALL_CATEGORY,
            PrefManager.getCallCategory().ordinal
        )]

        Log.d(TAG, "getArguments: ${vm.source}  ${vm.callType}")

        when (vm.source) {
            FROM_CALL_BAR -> {}
            FROM_INCOMING_CALL -> {
                val incomingCallId = PrefManager.getIncomingCallId()
                Log.d(TAG, "getArguments: $incomingCallId")
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
                setCallData()
            }
        }
    }

    private fun setCallData() {
        when (vm.callType) {
            com.joshtalks.joshskills.premium.calling.constant.Category.PEER_TO_PEER -> {
                val topicId = intent?.getStringExtra(INTENT_DATA_TOPIC_ID)
                val courseId = intent?.getStringExtra(INTENT_DATA_COURSE_ID)
                voipLog?.log("Call Data --> $intent")
                vm.callData[INTENT_DATA_COURSE_ID] = courseId ?: "0"
                vm.callData[INTENT_DATA_TOPIC_ID] = topicId ?: "0"
            }
            com.joshtalks.joshskills.premium.calling.constant.Category.FPP -> {
                val mentorId = intent?.getStringExtra(INTENT_DATA_FPP_MENTOR_ID)
                vm.callData[INTENT_DATA_FPP_MENTOR_ID] = mentorId ?: "0"
                vm.callData[INTENT_DATA_FPP_NAME] =
                    intent?.getStringExtra(INTENT_DATA_FPP_NAME).toString()
            }
            com.joshtalks.joshskills.premium.calling.constant.Category.GROUP -> {
                val topicId = intent?.getStringExtra(INTENT_DATA_TOPIC_ID)
                val groupId = intent?.getStringExtra(INTENT_DATA_GROUP_ID)
                vm.callData[INTENT_DATA_TOPIC_ID] = topicId ?: "0"
                vm.callData[INTENT_DATA_GROUP_ID] = groupId ?: "0"
            }
            com.joshtalks.joshskills.premium.calling.constant.Category.EXPERT -> {
                val mentorId = intent?.getStringExtra(INTENT_DATA_FPP_MENTOR_ID)
                vm.callData[INTENT_DATA_FPP_MENTOR_ID] = mentorId ?: "0"
                vm.callData[INTENT_DATA_EXPERT_PRICE_PER_MIN] =
                    intent?.getStringExtra(INTENT_DATA_EXPERT_PRICE_PER_MIN).toString()
                vm.callData[INTENT_DATA_TOTAL_AMOUNT] =
                    intent?.getStringExtra(INTENT_DATA_TOTAL_AMOUNT).toString()
                vm.callData[IS_EXPERT_CALLING] =
                    intent?.getStringExtra(IS_EXPERT_CALLING).toString()
                vm.callData[INTENT_DATA_FPP_NAME] =
                    intent?.getStringExtra(INTENT_DATA_FPP_NAME).toString()
                vm.callData[INTENT_DATA_EXPERT_PREMIUM] =
                    intent?.getBooleanExtra(INTENT_DATA_EXPERT_PREMIUM, false) ?: false
            }
        }
    }

    private fun getSource(): String {
        val topicId = intent?.getStringExtra(INTENT_DATA_TOPIC_ID)
        val mentorId = intent?.getStringExtra(INTENT_DATA_FPP_MENTOR_ID)
        val groupId = intent?.getStringExtra(INTENT_DATA_GROUP_ID)
        Log.d(TAG, "getSource: $topicId  $mentorId  $groupId")

        val shouldOpenCallFragment =
            ((topicId == EMPTY || topicId == null) && mentorId == null && groupId == null)
        return if (shouldOpenCallFragment && PrefManager.getVoipState() == com.joshtalks.joshskills.premium.calling.constant.State.IDLE)
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
        startCallingScreen()
    }

    override fun onResume() {
        supportActionBar?.hide()
        super.onResume()
    }

    private fun startCallingScreen() {
        when (vm.callType) {
            com.joshtalks.joshskills.premium.calling.constant.Category.PEER_TO_PEER -> {
                openFragment { addCallUserFragment() }
            }
            com.joshtalks.joshskills.premium.calling.constant.Category.FPP -> {
                addFppCallFragment()
            }
            com.joshtalks.joshskills.premium.calling.constant.Category.GROUP -> {
                openFragment { addGroupCallFragment() }
            }
            com.joshtalks.joshskills.premium.calling.constant.Category.EXPERT -> {
                addExpertCallFragment()
            }
        }
    }

    private inline fun openFragment(fragment: () -> Unit) {
        if (vm.source == FROM_INCOMING_CALL || vm.source == FROM_CALL_BAR) {
            fragment.invoke()
        } else if (vm.source == FROM_ACTIVITY) {
            addSearchingUserFragment()
        }
    }

    override fun initViewState() {
        event.observe(this) {
            Log.i(TAG, "initViewState: event -> ${it.what}")
            when (it.what) {
                com.joshtalks.joshskills.premium.calling.constant.CALL_INITIATED_EVENT -> {
                    when (vm.callType) {
                        com.joshtalks.joshskills.premium.calling.constant.Category.PEER_TO_PEER -> {
                            replaceCallUserFragment()
                        }
                        com.joshtalks.joshskills.premium.calling.constant.Category.GROUP -> {
                            replaceGroupUserFragment()
                        }
                        else -> {}
                    }
                }
                com.joshtalks.joshskills.premium.calling.constant.CLOSE_CALL_SCREEN -> {
                    val intent = Intent().apply {
                        putExtra(CALL_DURATION, it.obj as? Long)
                    }
                    setResult(RESULT_OK, intent)
                    finishAndRemoveTask()
                }
                com.joshtalks.joshskills.premium.calling.constant.SHOW_DISCONNECT_DIALOG -> {
                    showDialog()
                }

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

    private fun addExpertCallFragment() {
        supportFragmentManager.commit {
            add(R.id.voice_call_container, ExpertCallFragment(), "ExpertCallFragment")
        }
    }


    private fun replaceGroupUserFragment() {
        supportFragmentManager.commit {
            replace(R.id.voice_call_container, GroupCallFragment(), "GroupFragment")
        }
    }

    private fun addGroupCallFragment() {
        supportFragmentManager.commit {
            add(R.id.voice_call_container, GroupCallFragment(), "GroupFragment")
        }
    }


    private fun replaceCallUserFragment() {
        supportFragmentManager.commit {
            replace(R.id.voice_call_container, CallFragment(), "CallFragment")
        }
    }

    override fun onStart() {
        super.onStart()
        if (isCallingPermissionEnabled(this)) {
            if (!isServiceBounded) {
                vm.boundService(this)
                isServiceBounded = true
            }
        } else {
            getPermissions()
        }
    }

    override fun onStop() {
        super.onStop()
        if (isServiceBounded) {
            vm.unboundService(this)
            isServiceBounded = false
        }
    }

    override fun onBackPressed() {
        if (PrefManager.getVoipState() == com.joshtalks.joshskills.premium.calling.constant.State.IDLE || PrefManager.getVoipState() == com.joshtalks.joshskills.premium.calling.constant.State.SEARCHING || PrefManager.getVoipState() == com.joshtalks.joshskills.premium.calling.constant.State.JOINING || PrefManager.getVoipState() == com.joshtalks.joshskills.premium.calling.constant.State.JOINED)
            backPressMutex.onMultipleBackPress {
                super.onBackPressed()
                vm.backPress()
            }
        else {
            super.onBackPressed()
            vm.backPress()
        }
    }

    private fun showDialog() {
        if (::dialog.isInitialized)
            dialog.dismiss()
        val dialogTitle = AppObjectController.getFirebaseRemoteConfig()
            .getString(
                "${FirebaseRemoteConfigKey.DISCONNECT_DIALOG_TITLE}${CorePrefManager.getStringValue(
                    com.joshtalks.joshskills.premium.core.CURRENT_COURSE_ID
                )}"
            )

        val builder = AlertDialog.Builder(this).apply {
            if(vm.callType == com.joshtalks.joshskills.premium.calling.constant.Category.PEER_TO_PEER)
                setTitle( dialogTitle.ifBlank { getString(R.string.default_disconnect_dialog_title) })
            if(vm.callType == com.joshtalks.joshskills.premium.calling.constant.Category.PEER_TO_PEER)
                setMessage(
                    getConformationDialogMessage()
                )
            else
                setMessage(dialogTitle.ifBlank { getString(R.string.default_disconnect_dialog_title) })
            setCancelable(true)
            setPositiveButton("No") { p0, p1 ->
                CallAnalytics.addAnalytics(
                    event = EventName.DISCONNECT_DIALOG_NO_PRESSED,
                    agoraCallId = PrefManager.getAgraCallId().toString(),
                    agoraMentorId = PrefManager.getLocalUserAgoraId().toString(),
                    extra = PrefManager.getVoipState().name
                )
                dialog.dismiss()
            }
            setNegativeButton("Yes") { p0, p1 ->
                CallAnalytics.addAnalytics(
                    event = EventName.DISCONNECTED_BY_DIALOG,
                    agoraCallId = PrefManager.getAgraCallId().toString(),
                    agoraMentorId = PrefManager.getLocalUserAgoraId().toString(),
                    extra = PrefManager.getVoipState().name
                )
                vm.disconnect()
            }
        }
        dialog = builder.create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.RED);
        }
        dialog.show()
        CallAnalytics.addAnalytics(
            event = EventName.DISCONNECT_DIALOG_SHOWN,
            agoraCallId = PrefManager.getAgraCallId().toString(),
            agoraMentorId = PrefManager.getLocalUserAgoraId().toString(),
            extra = PrefManager.getVoipState().name
        )
    }

    private fun getConformationDialogMessage() : String {
        return AppObjectController.getFirebaseRemoteConfig()
            .getString(
                "${FirebaseRemoteConfigKey.DISCONNECT_DIALOG_TEXT}${CorePrefManager.getStringValue(
                    com.joshtalks.joshskills.premium.core.CURRENT_COURSE_ID
                )}"
            ).ifBlank { getString(R.string.default_disconnect_dialog_text) }
    }
}

data class AutoCallData(val heading: String = "", val subHeading: String = "", )