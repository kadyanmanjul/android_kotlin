package com.joshtalks.joshskills.ui.voip.new_arch.ui.views

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Environment
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.commit
import androidx.lifecycle.ViewModelProvider
import com.hbisoft.hbrecorder.HBRecorder
import com.hbisoft.hbrecorder.HBRecorderListener
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.base.BaseActivity
import com.joshtalks.joshskills.base.constants.*
import com.joshtalks.joshskills.core.EMPTY
import com.joshtalks.joshskills.core.PermissionUtils.callingPermissionPermanentlyDeniedDialog
import com.joshtalks.joshskills.core.PermissionUtils.isCallingPermissionEnabled
import com.joshtalks.joshskills.databinding.ActivityVoiceCallBinding
import com.joshtalks.joshskills.ui.voip.new_arch.ui.viewmodels.VoiceCallViewModel
import com.joshtalks.joshskills.ui.voip.new_arch.ui.viewmodels.voipLog
import com.joshtalks.joshskills.voip.Utils
import com.joshtalks.joshskills.voip.Utils.Companion.onMultipleBackPress
import com.joshtalks.joshskills.voip.constant.*
import com.joshtalks.joshskills.voip.data.CallingRemoteService
import com.joshtalks.joshskills.voip.data.local.PrefManager
import com.joshtalks.joshskills.voip.voipanalytics.CallAnalytics
import com.joshtalks.joshskills.voip.voipanalytics.EventName
import kotlinx.coroutines.sync.Mutex
import java.io.File

private const val TAG = "VoiceCallActivity"

class VoiceCallActivity : BaseActivity(),HBRecorderListener {
    private val backPressMutex = Mutex(false)
    private var isServiceBounded = false
    private val SCREEN_RECORD_REQUEST_CODE = 1
    var recordingPermissionAlert: AlertDialog? = null
    var file : File? = null
    var currentFileName : String? = null

    companion object{
        var showDialog = true
        var recordData : Intent? = null
        var recordResultCode : Int? = null
    }

    private val voiceCallBinding by lazy<ActivityVoiceCallBinding> {
        DataBindingUtil.setContentView(this, R.layout.activity_voice_call)
    }
    private val hbRecorder by lazy {
        HBRecorder(this,this)
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
                Toast.makeText(applicationContext,"Please Allow Permissions to make call",Toast.LENGTH_LONG).show()
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
            requestPermissionsLauncher.launch(
                arrayOf(
                    Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.READ_PHONE_STATE,
                    Manifest.permission.ACCESS_NETWORK_STATE,
                    Manifest.permission.MODIFY_AUDIO_SETTINGS,
                )
            )
        }else{
            callingPermissionPermanentlyDeniedDialog(
                this,
                message = R.string.call_start_permission_message
            )
        }
    }

    override fun getArguments() {
        vm.source = getSource()
        vm.callType = Category.values()[intent.getIntExtra(INTENT_DATA_CALL_CATEGORY, PrefManager.getCallCategory().ordinal)]

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
            Category.PEER_TO_PEER -> {
                val topicId = intent?.getStringExtra(INTENT_DATA_TOPIC_ID)
                val courseId = intent?.getStringExtra(INTENT_DATA_COURSE_ID)
                voipLog?.log("Call Data --> $intent")
                vm.callData[INTENT_DATA_COURSE_ID] = courseId ?: "0"
                vm.callData[INTENT_DATA_TOPIC_ID] = topicId ?: "0"
            }
            Category.FPP ->{
                val mentorId = intent?.getStringExtra(INTENT_DATA_FPP_MENTOR_ID)
                vm.callData[INTENT_DATA_FPP_MENTOR_ID] = mentorId ?: "0"
            }
            Category.GROUP -> {
                val topicId = intent?.getStringExtra(INTENT_DATA_TOPIC_ID)
                val groupId = intent?.getStringExtra(INTENT_DATA_GROUP_ID)
                vm.callData[INTENT_DATA_TOPIC_ID] = topicId ?: "0"
                vm.callData[INTENT_DATA_GROUP_ID] = groupId ?: "0"}
        }
    }

    private fun getSource(): String {
        val topicId = intent?.getStringExtra(INTENT_DATA_TOPIC_ID)
        val mentorId = intent?.getStringExtra(INTENT_DATA_FPP_MENTOR_ID)
        val groupId = intent?.getStringExtra(INTENT_DATA_GROUP_ID)
        Log.d(TAG, "getSource: $topicId  $mentorId  $groupId")

        val shouldOpenCallFragment = ((topicId == EMPTY || topicId==null) && mentorId == null && groupId == null )
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
        startCallingScreen()
    }

    override fun onResume() {
        supportActionBar?.hide()
        super.onResume()
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
                openFragment { addGroupCallFragment() }
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
            Log.i(TAG, "initViewState: event -> ${it.what}")
            when (it.what) {
                CALL_INITIATED_EVENT -> {
                    when (vm.callType) {
                        Category.PEER_TO_PEER ->{
                            replaceCallUserFragment()
                        }
                        Category.GROUP -> {
                            replaceGroupUserFragment()
                        }
                        else -> {}
                    }
                }
                CLOSE_CALL_SCREEN -> {
                    finishAndRemoveTask()
                }
                START_SCREEN_RECORDING->{
                    startScreenRecording()
                }
                STOP_SCREEN_RECORDING->{
                    stopScreenRecording()
                }
                CHANGE_APP_THEME_T0_BLACK->{
                    window.statusBarColor  = ContextCompat.getColor(this,R.color.black_quiz)
                }
                CHANGE_APP_THEME_T0_BLUE->{
                    window.statusBarColor  = ContextCompat.getColor(this,R.color.colorPrimaryDark)
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
        if(isCallingPermissionEnabled(this)) {
            if(!isServiceBounded) {
                vm.boundService(this)
                isServiceBounded = true
            }
        }else{
            getPermissions()
        }
    }

    override fun onStop() {
        super.onStop()
        if(isServiceBounded) {
            vm.unboundService(this)
            isServiceBounded = false
        }
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

    private fun initiateRecording() {
        val mediaProjectionManager: MediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        val permissionIntent: Intent? = mediaProjectionManager.createScreenCaptureIntent()
        startActivityForResult(permissionIntent, SCREEN_RECORD_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == SCREEN_RECORD_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                recordData = data
                recordResultCode = resultCode
                showDialog = false
                proceedToRecord()
            }else{
               vm.endGameFromRepo()
            }
        }
    }

    fun stopScreenRecording(){
        hbRecorder.stopScreenRecording()
    }

    fun startScreenRecording(){
        if(showDialog){
            initiateRecording()
        }else{
            proceedToRecord()
        }
    }

    private fun proceedToRecord() {
        vm.isRecordPermissionGiven.set(true)

        hbRecorder.setOutputPath(getFileDirectory().absolutePath)
        hbRecorder.isAudioEnabled(false)
        hbRecorder.recordHDVideo(false)
        currentFileName = "recordVideo" + "${System.currentTimeMillis()}"
        hbRecorder.fileName = currentFileName
        recordResultCode?.let { hbRecorder.startScreenRecording(recordData, it)
        }
    }

    private fun getFileDirectory(): File {
        val dir = application.getExternalFilesDir(Environment.DIRECTORY_MOVIES)
        file = File(dir?.path + "/JoshSkills")
        if(file?.exists()==false){
            file?.mkdirs()
        }
        return file as File
    }

    override fun HBRecorderOnStart() {
    }

    override fun HBRecorderOnComplete() {
        val file1 = File(file, "$currentFileName.mp4")
        vm.processRecording(videoFile = file1)
        file = null
        currentFileName = null
    }

    override fun HBRecorderOnError(errorCode: Int, reason: String?) {
        Log.d(TAG, "HBRecorderOnError: $errorCode $reason")
    }
}