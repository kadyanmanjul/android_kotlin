package com.joshtalks.joshskills.ui.voip.new_arch.ui.viewmodels

import android.app.Activity
import android.app.Application
import android.os.CountDownTimer
import android.os.Message
import android.util.Log
import android.view.View
import androidx.databinding.ObservableArrayList
import androidx.databinding.ObservableField
import androidx.databinding.ObservableInt
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.base.EventLiveData
import com.joshtalks.joshskills.base.constants.FPP
import com.joshtalks.joshskills.base.constants.FROM_INCOMING_CALL
import com.joshtalks.joshskills.base.constants.GROUP
import com.joshtalks.joshskills.base.constants.PEER_TO_PEER
import com.joshtalks.joshskills.base.log.Feature
import com.joshtalks.joshskills.base.log.JoshLog
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.ui.call.repository.RepositoryConstants.CONNECTION_ESTABLISHED
import com.joshtalks.joshskills.ui.call.repository.WebrtcRepository
import com.joshtalks.joshskills.ui.voip.new_arch.ui.call_recording.ProcessCallRecordingService
import com.joshtalks.joshskills.ui.voip.new_arch.ui.models.CallUIState
import com.joshtalks.joshskills.ui.voip.util.ScreenViewRecorder
import com.joshtalks.joshskills.voip.Utils
import com.joshtalks.joshskills.voip.constant.CALL_CONNECTED_EVENT
import com.joshtalks.joshskills.voip.constant.CALL_INITIATED_EVENT
import com.joshtalks.joshskills.voip.constant.CANCEL_INCOMING_TIMER
import com.joshtalks.joshskills.voip.constant.CLOSE_CALL_SCREEN
import com.joshtalks.joshskills.voip.constant.HIDE_RECORDING_PERMISSION_DIALOG
import com.joshtalks.joshskills.voip.constant.RECONNECTING_FAILED
import com.joshtalks.joshskills.voip.constant.SHOW_RECORDING_PERMISSION_DIALOG
import com.joshtalks.joshskills.voip.constant.SHOW_RECORDING_REJECTED_DIALOG
import com.joshtalks.joshskills.voip.constant.State
import com.joshtalks.joshskills.voip.data.RecordingButtonState
import com.joshtalks.joshskills.voip.data.ServiceEvents
import com.joshtalks.joshskills.voip.data.local.PrefManager
import com.joshtalks.joshskills.voip.getVideoUrl
import com.joshtalks.joshskills.voip.recordinganalytics.CallRecordingAnalytics
import com.joshtalks.joshskills.voip.voipanalytics.CallAnalytics
import com.joshtalks.joshskills.voip.voipanalytics.EventName
import kotlinx.coroutines.*
import java.io.File
import java.util.ArrayDeque
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

const val CONNECTING = 1
const val ONGOING = 2

val voipLog = JoshLog.getInstanceIfEnable(Feature.VOIP)

class VoiceCallViewModel(val applicationContext: Application) : AndroidViewModel(applicationContext) {
    private val TAG = "VoiceCallViewModel"
    private var isConnectionRequestSent = false
    lateinit var source: String
    private val repository = WebrtcRepository(viewModelScope)
    private val mutex = Mutex(false)
    val callType = ObservableField("")
    val callStatus = ObservableInt(getCallStatus())
    var imageList = ObservableArrayList<String>()
    val callData = HashMap<String, Any>()
    val uiState by lazy { CallUIState() }
    val pendingEvents = ArrayDeque<Int>()
    var recordCnclStop = 0 // 0 = record, 1 = cancel, 2 = stop
    private var singleLiveEvent = EventLiveData
    var audioRecordFile: File? = null
    var videoRecordFile: File? = null
    var visibleCrdView = false
    var isListening = false
    var isRequestDialogShowed = false
    var timer: CountDownTimer? = null

    private val connectCallJob by lazy {
        viewModelScope.launch(start = CoroutineStart.LAZY) {
            mutex.withLock {
                if (PrefManager.getVoipState() == State.IDLE && isConnectionRequestSent.not()) {
                    Log.d(TAG, " connectCallJob : Inside - $callData")
                    repository.connectCall(callData)
                    isConnectionRequestSent = true
                }
            }
        }
    }

    init {
        listenRepositoryEvents()
    }

    fun listenRepositoryEvents() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.observeRepositoryEvents().collect {
                when (it) {
                    CONNECTION_ESTABLISHED -> {
                        if (isListening.not()) {
                            isListening  = true
                            listenUIState()
                            listenVoipEvents()
                            connectCall()
                        }
                    }
                }
            }
        }
    }

    private fun listenVoipEvents() {
        Log.d(TAG, "listenVoipEvents: ")
        viewModelScope.launch(Dispatchers.IO) {
            repository.observerVoipEvents()?.collect {
                // TODO: Manage Local State
                when (it) {
                    ServiceEvents.CALL_INITIATED_EVENT -> {
                        uiState.currentState = "Connecting..."
                        val msg = Message.obtain().apply {
                            what = if (source == FROM_INCOMING_CALL)
                                CANCEL_INCOMING_TIMER
                            else
                                CALL_INITIATED_EVENT
                        }
                        withContext(Dispatchers.Main) {
                            singleLiveEvent.value = msg
                        }
                    }
                    ServiceEvents.CALL_CONNECTED_EVENT -> {
                        uiState.currentState = "Timer"
                        if (source != FROM_INCOMING_CALL) {
                            val msg = Message.obtain().apply {
                                what = CALL_CONNECTED_EVENT
                            }
                            withContext(Dispatchers.Main) {
                                singleLiveEvent.value = msg
                            }
                        }
                    }
                    ServiceEvents.CLOSE_CALL_SCREEN -> {
                        Log.i("call_cut", "listenVoipEvents: $it")
                        val msg = Message.obtain().apply {
                            what = CLOSE_CALL_SCREEN
                        }
                        withContext(Dispatchers.Main) {
                            singleLiveEvent.value = msg
                        }
                        if (uiState.recordingButtonState == RecordingButtonState.RECORDING) {
                            stopAudioVideoRecording()
                        }
                    }
                    ServiceEvents.RECONNECTING_FAILED -> {
                        val msg = Message.obtain().apply {
                            what = RECONNECTING_FAILED
                        }
                        withContext(Dispatchers.Main) {
                            singleLiveEvent.value = msg
                        }
                    }
                    // remote user event
                    ServiceEvents.START_RECORDING -> {
                        /*val msg = Message.obtain().apply {
                            what = SHOW_RECORDING_PERMISSION_DIALOG
                        }
                        withContext(Dispatchers.Main) {
                            singleLiveEvent.value = msg
                        }*/
                    }
                    ServiceEvents.STOP_RECORDING -> {
                        stopAudioVideoRecording()
                        stoppedRecUIchanges()
                    }
                    ServiceEvents.CALL_RECORDING_ACCEPT -> {
                        recordCnclStop = 2
                        //startRecording()
                        val msg = Message.obtain().apply {
                            what = HIDE_RECORDING_PERMISSION_DIALOG
                            obj = true
                        }
                        withContext(Dispatchers.Main) {
                            singleLiveEvent.value = msg
                        }
                    }
                    ServiceEvents.CALL_RECORDING_REJECT -> {
                        stoppedRecUIchanges()
                        val msg = Message.obtain().apply {
                            what = SHOW_RECORDING_REJECTED_DIALOG
                        }
                        withContext(Dispatchers.Main) {
                            singleLiveEvent.value = msg
                        }
                    }
                    ServiceEvents.CANCEL_RECORDING_REQUEST -> {
                        Log.d(TAG, "listenVoipEvents: cancelled!")

                        val msg = Message.obtain().apply {
                            what = HIDE_RECORDING_PERMISSION_DIALOG
                        }
                        withContext(Dispatchers.Main) {
                            singleLiveEvent.value = msg
                        }
                    }
                    ServiceEvents.PROCESS_AGORA_CALL_RECORDING -> {
                        Log.d(TAG, "listenVoipEvents() called")
                        File(PrefManager.getLastRecordingPath())?.let { file ->
                            Log.e("sagar", "listenVoipEvents: $file" )
                            stopRecording(file)
                            timer?.cancel()
                        }
                    }
                }
            }
        }
    }

    fun recordingStartedUIChanges() {
        uiState.visibleCrdView = true
        uiState.recordCrdViewTxt = "REC"
        uiState.recordBtnImg = R.drawable.ic_stop_record
        uiState.recordBtnTxt = "Stop"
    }

    fun stopRecording(recordFile: File) {
        viewModelScope.launch(Dispatchers.IO) {
            if (recordFile.absolutePath.isEmpty().not()) {

                val toastText  = AppObjectController.getFirebaseRemoteConfig().getString("RECORDING_SAVED_TEXT")
                withContext(Dispatchers.Main) {
                    Utils.showToast(toastText)
                }
                val len = recordFile.length()
                if (len < 1) {
                    return@launch
                }

                ProcessCallRecordingService.processSingleCallRecording(context = Utils.context, videoPath = videoRecordFile?.absolutePath?:"", audioPath = recordFile.absolutePath, callId = PrefManager.getAgraCallId().toString(),agoraMentorId =PrefManager.getLocalUserAgoraId().toString())
                CallRecordingAnalytics.addAnalytics(
                    agoraCallId = PrefManager.getAgraCallId().toString(),
                    agoraMentorId =PrefManager.getLocalUserAgoraId().toString(),
                    localPath = recordFile.absolutePath )
            }
        }
    }

    fun acceptCallRecording(view:View) {
        repository.acceptCallRecording()
        CallAnalytics.addAnalytics(
            event = EventName.RECORDING_ACCEPTED,
            agoraCallId = PrefManager.getAgraCallId().toString(),
            agoraMentorId = PrefManager.getLocalUserAgoraId().toString()
        )
        startAudioVideoRecording(view)
    }

    fun startAudioVideoRecording(view: View){
        videoRecordFile = File(Utils.context?.getVideoUrl()?:"")
        Log.e("sagar", "startAudioVideoRecording: $videoRecordFile" )
        ScreenViewRecorder.videoRecorder.startPlayer(videoRecordFile?.absolutePath?:"", Utils.context!!,view)
        repository.startAgoraRecording()
    }

    fun stopAudioVideoRecording(){
        ScreenViewRecorder.videoRecorder.stopPlaying()
        repository.stopAgoraClientCallRecording()
    }

    fun rejectCallRecording() {
        CallAnalytics.addAnalytics(
            event = EventName.RECORDING_REJECTED,
            agoraCallId = PrefManager.getAgraCallId().toString(),
            agoraMentorId = PrefManager.getLocalUserAgoraId().toString()
        )
        repository.rejectCallRecording()
    }

    private fun listenUIState() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.observeUserDetails()?.collect { state ->
                Log.d(TAG, "listenUIState: $state")
                val voipState = PrefManager.getVoipState()
                Log.d(TAG, "listenUIState: State --> $voipState")
                if (uiState.startTime != state.startTime)
                    uiState.startTime = state.startTime
                uiState.isRecordingEnabled = state.isRecordingEnabled
                // TODO remove this logic from here ( issues: fix when state is REQUESTED we have to show dialog even when other user come back from background )
                if (state.recordingButtonState == RecordingButtonState.REQUESTED
                    && uiState.recordingButtonState != RecordingButtonState.REQUESTED
                    && uiState.recordBtnTxt.equals("Record")) {
                    val msg = Message.obtain().apply {
                        what = SHOW_RECORDING_PERMISSION_DIALOG
                    }
                    withContext(Dispatchers.Main) {
                        singleLiveEvent.value = msg
                    }
                }
                uiState.recordingButtonState = state.recordingButtonState
                if (uiState.recordTime != state.recordingStartTime)
                    uiState.recordTime = state.recordingStartTime
                withContext(Dispatchers.Main) {
                    timer = getTime(state.recordingButtonState)
                    timer?.start()
                }
                uiState.name = state.remoteUserName
                uiState.profileImage = state.remoteUserImage ?: ""
                uiState.topic = state.topicName
                uiState.topicImage = state.currentTopicImage
                uiState.type = state.callType
                uiState.occupation = getOccupationText(state.aspiration, state.occupation)
                uiState.title = when (state.callType) {
                    PEER_TO_PEER -> "Practice with Partner"
                    FPP -> "Favorite Practice Partner"
                    GROUP -> "Group Call"
                    else -> ""
                }

                when(state.recordingButtonState) {
                    RecordingButtonState.IDLE -> stoppedRecUIchanges()
                    RecordingButtonState.REQUESTED -> recWaitingForUserUI()
                    RecordingButtonState.RECORDING -> recordingStartedUIChanges()
                }

                if (state.isReconnecting) {
                    uiState.currentState = "Reconnecting"
                } else if (state.isOnHold) {
                    uiState.currentState = "Call on Hold"
                    voipLog?.log("HOLD")
                } else if (state.isRemoteUserMuted) {
                    uiState.currentState = "User Muted the Call"
                    voipLog?.log("Mute")
                } else {
                    if (voipState == State.CONNECTED || voipState == State.RECONNECTING)
                        uiState.currentState = "Timer"
                }

                if (uiState.isSpeakerOn != state.isSpeakerOn) {
                    if (state.isSpeakerOn) {
                        uiState.isSpeakerOn = true
                        repository.turnOnSpeaker()
                    } else {
                        uiState.isSpeakerOn = false
                        repository.turnOffSpeaker()
                    }
                }

                if (uiState.isMute != state.isOnMute) {
                    Log.d(TAG, "listenUIState: MUTE -- ${state.isOnMute}")
                    if (state.isOnMute) {
                        uiState.isMute = true
                        repository.muteCall()
                    } else {
                        uiState.isMute = false
                        repository.unmuteCall()
                    }
                }
            }
        }
    }

    fun getTime(recordingButtonState: RecordingButtonState): CountDownTimer? {
        try {
            timer = object : CountDownTimer(60000, 1000) {
                override fun onTick(millisUntilFinished: Long) {}

                override fun onFinish() {
                    if (recordingButtonState == RecordingButtonState.RECORDING) {
                        if (uiState.recordTime > 0) {
                            recordingStopButtonClickListener()
                        }
                    }
                }
            }
        } catch (ex: Exception) {
            Log.e("sagar", "onFinish: Call2${ex.message}")
        }
        return timer
    }

    private fun getOccupationText(aspiration: String, occupation: String): String {
        if (!checkIfNullOrEmpty(occupation) && !checkIfNullOrEmpty(aspiration)) {
            return "$occupation, Dream - $aspiration"
        } else if (checkIfNullOrEmpty(occupation) && !checkIfNullOrEmpty(aspiration)) {
            return "Dream - $aspiration"
        } else if (!checkIfNullOrEmpty(occupation) && checkIfNullOrEmpty(aspiration)) {
            return occupation
        }
        return ""
    }

    private fun checkIfNullOrEmpty(word: String): Boolean {
        return word == "" || word == "null"
    }

    private fun getCallStatus(): Int {
        val status = PrefManager.getVoipState()
        return if (status == State.CONNECTED) {
            ONGOING
        } else {
            CONNECTING
        }
    }

    private fun connectCall() {
        Log.d(TAG, "connectCall: ")
        connectCallJob.start()
    }

    // User Action
    fun disconnectCall(v: View) {
        Log.d(TAG, "Disconnect Call :Red Button Press")
        CallAnalytics.addAnalytics(
            event = EventName.DISCONNECTED_BY_RED_BUTTON,
            agoraCallId = PrefManager.getAgraCallId().toString(),
            agoraMentorId = PrefManager.getLocalUserAgoraId().toString(),
            extra = PrefManager.getVoipState().name
        )
        disconnect()
    }

    fun disconnect() {
        Log.d(TAG, "disconnect: ")
        repository.disconnectCall()
    }

    // User Action
    fun switchSpeaker(v: View) {
        Log.d(TAG, "switchSpeaker")
        val isOnSpeaker = uiState.isSpeakerOn
        uiState.isSpeakerOn = isOnSpeaker.not()
        if (isOnSpeaker) {
            CallAnalytics.addAnalytics(
                event = EventName.SPEAKER_OFF,
                agoraCallId = PrefManager.getAgraCallId().toString(),
                agoraMentorId = PrefManager.getLocalUserAgoraId().toString()
            )
            repository.turnOffSpeaker()
        } else {
            CallAnalytics.addAnalytics(
                event = EventName.SPEAKER_ON,
                agoraCallId = PrefManager.getAgraCallId().toString(),
                agoraMentorId = PrefManager.getLocalUserAgoraId().toString()
            )
            repository.turnOnSpeaker()
        }
    }

    // User Action
    fun recordCall(v: View) {
        Log.d(TAG, "recordCall")
        when(uiState.recordingButtonState) {
            RecordingButtonState.IDLE -> {
                CallAnalytics.addAnalytics(
                    event = EventName.RECORDING_INITIATED,
                    agoraCallId = PrefManager.getAgraCallId().toString(),
                    agoraMentorId = PrefManager.getLocalUserAgoraId().toString()
                )
                recWaitingForUserUI()
                repository.startCallRecording()
            }
            RecordingButtonState.REQUESTED -> {
                if (uiState.recordTime == 0L) {
                    Log.i(TAG, "recordCall: cancelled")
                    repository.cancelRecordingRequest()
                }
                CallAnalytics.addAnalytics(
                    event = EventName.RECORDING_STOPPED,
                    agoraCallId = PrefManager.getAgraCallId().toString(),
                    agoraMentorId = PrefManager.getLocalUserAgoraId().toString()
                )
                stoppedRecUIchanges()
                stopAudioVideoRecording()
                repository.stopCallRecording()
            }
            RecordingButtonState.RECORDING -> {
                recordingStopButtonClickListener()
            }
        }
    }

    fun recordingStopButtonClickListener(){
        CallAnalytics.addAnalytics(
            event = EventName.RECORDING_STOPPED,
            agoraCallId = PrefManager.getAgraCallId().toString(),
            agoraMentorId = PrefManager.getLocalUserAgoraId().toString()
        )
        stoppedRecUIchanges()
        stopAudioVideoRecording()
        repository.stopCallRecording()
    }

    private fun stoppedRecUIchanges() {
        uiState.recordBtnTxt = "Record"
        uiState.recordBtnImg = R.drawable.call_fragment_record
        uiState.visibleCrdView = false
    }

    private fun recWaitingForUserUI() {
        uiState.recordCrdViewTxt = "Waiting for your partner to accept"
        uiState.visibleCrdView = true
        uiState.recordBtnImg = R.drawable.ic_cancel_record
        uiState.recordBtnTxt = "Cancel"
    }

    // User Action
    fun switchMic(v: View) {
        Log.d(TAG, "switchMic")
        val isOnMute = uiState.isMute
        uiState.isMute = isOnMute.not()
        if (isOnMute) {
            CallAnalytics.addAnalytics(
                event = EventName.MIC_ON,
                agoraCallId = PrefManager.getAgraCallId().toString(),
                agoraMentorId = PrefManager.getLocalUserAgoraId().toString()
            )
            repository.unmuteCall()
        } else {
            CallAnalytics.addAnalytics(
                event = EventName.MIC_OFF,
                agoraCallId = PrefManager.getAgraCallId().toString(),
                agoraMentorId = PrefManager.getLocalUserAgoraId().toString()
            )
            repository.muteCall()
        }
    }

    // User Action
    fun backPress() {
        Log.d(TAG, "backPress ")
        repository.backPress()
    }

    fun getNewTopicImage(v: View) {
        if (Utils.isInternetAvailable().not()) {
            Utils.showToast("Seems like you have no internet")
            return
        }
        CallAnalytics.addAnalytics(
            event = EventName.NEXT_TOPIC_BTN_PRESS,
            agoraCallId = PrefManager.getAgraCallId().toString(),
            agoraMentorId = PrefManager.getLocalUserAgoraId().toString()
        )
        repository.getNewTopicImage()
    }

    fun boundService(activity: Activity) {
        voipLog?.log("binding Service")
        repository.startService(activity)
    }

    fun unboundService(activity: Activity) {
        voipLog?.log("unbound Service")
        repository.stopService(activity)
    }

    override fun onCleared() {
        Log.d(TAG, "onCleared: ")
        super.onCleared()
        repository.clearRepository()
    }

    fun saveImageAudioToFolder(imageFile: File) {
        // TODO save another file in downloads folder with merged audio and Screenshot
    }
}