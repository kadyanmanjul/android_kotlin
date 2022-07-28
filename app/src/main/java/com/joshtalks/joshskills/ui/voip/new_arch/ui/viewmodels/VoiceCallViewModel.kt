package com.joshtalks.joshskills.ui.voip.new_arch.ui.viewmodels

import android.app.Activity
import android.app.Application
import android.os.Message
import android.os.SystemClock
import android.util.Log
import android.view.View
import androidx.databinding.Observable
import androidx.databinding.ObservableArrayList
import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableInt
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.base.EventLiveData
import com.joshtalks.joshskills.base.constants.*
import com.joshtalks.joshskills.base.log.Feature
import com.joshtalks.joshskills.base.log.JoshLog
import com.joshtalks.joshskills.quizgame.base.GameEventLiveData
import com.joshtalks.joshskills.ui.call.repository.RepositoryConstants.CONNECTION_ESTABLISHED
import com.joshtalks.joshskills.ui.call.repository.WebrtcRepository
import com.joshtalks.joshskills.ui.voip.new_arch.ui.call_recording.ProcessCallRecordingService
import com.joshtalks.joshskills.ui.voip.new_arch.ui.models.CallUIState
import com.joshtalks.joshskills.voip.Utils
import com.joshtalks.joshskills.voip.constant.*
import com.joshtalks.joshskills.ui.voip.util.ScreenViewRecorder
import com.joshtalks.joshskills.voip.*
import com.joshtalks.joshskills.voip.data.RecordingButtonState
import com.joshtalks.joshskills.voip.data.ServiceEvents
import com.joshtalks.joshskills.voip.data.local.PrefManager
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
    var callType : Category = Category.PEER_TO_PEER
    var isEnabled = ObservableBoolean(true)
    val callStatus = ObservableInt(getCallStatus())
    var imageList = ObservableArrayList<String>()
    val callData = HashMap<String, Any>()
    val uiState by lazy { CallUIState() }
    val pendingEvents = ArrayDeque<Int>()
    var recordCnclStop = 0 // 0 = record, 1 = cancel, 2 = stop
    private var singleLiveEvent = EventLiveData
    private var recordingLiveEvent = GameEventLiveData
    var audioRecordFile: File? = null
    var videoRecordFile: File? = null
    var visibleCrdView = false
    var isListening = false
    var isRequestDialogShowed = false
    var timer: Job? = null
    val toastText  = Utils.context?.getRecordingText()?:""
    var isPermissionGranted: ObservableBoolean = ObservableBoolean(false)
    var isGameEnabled: ObservableBoolean = ObservableBoolean(false)


    private val connectCallJob by lazy {
        viewModelScope.launch(start = CoroutineStart.LAZY) {
            mutex.withLock {
                if (PrefManager.getVoipState() == State.IDLE && isConnectionRequestSent.not()) {
                    Log.d(TAG, " connectCallJob : Inside - $callData  $callType")
                    repository.connectCall(callData,callType)
                    isConnectionRequestSent = true
                    if(callType==Category.FPP && source == FROM_ACTIVITY){
                        uiState.currentState = "Ringing..."
                    }
                }

            }
        }
    }

    init {
        isGameEnabled.set(Utils.context?.getGameFlag() == "1")
        listenRepositoryEvents()
       uiState.addOnPropertyChangedCallback(object : Observable.OnPropertyChangedCallback() {
            override fun onPropertyChanged(sender: Observable?, propertyId: Int) {
                if(propertyId==47) {
                    Log.d(TAG, "onPropertyChanged: $sender  $propertyId")
                }
            }
        })
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
        viewModelScope.launch(Dispatchers.IO) {
            repository.observerVoipEvents()?.collect {
                Log.d(TAG, "listenVoipEvents: $it")
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
                        val msg = Message.obtain().apply {
                            what = CLOSE_CALL_SCREEN
                        }
                        withContext(Dispatchers.Main) {
                            singleLiveEvent.value = msg
                        }
                        if (uiState.recordingButtonState == RecordingButtonState.RECORDING) {
                            cancelRecordingTimer()
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
                    ServiceEvents.PROCESS_AGORA_CALL_RECORDING -> {
                        Log.d(TAG, " GAME observe: listenVoipEvents() called")
                        File(PrefManager.getLastRecordingPath()).let { file ->
                            stopRecording(file)
                            cancelRecordingTimer()
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
                val len = recordFile.length()
                if (len < 1) {
                    return@launch
                }
                val currentTime = SystemClock.elapsedRealtime()
                ProcessCallRecordingService.processSingleCallRecording(context = Utils.context, videoPath = videoRecordFile?.absolutePath?:"", audioPath = recordFile.absolutePath, callId = PrefManager.getAgraCallId().toString(),agoraMentorId = PrefManager.getLocalUserAgoraId().toString(),recordDuration = ((currentTime - uiState.recordTime)/1000).toInt())
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
        try {
            videoRecordFile = File(Utils.context?.getVideoUrl()?:"")
            ScreenViewRecorder.videoRecorder.startPlayer(videoRecordFile?.absolutePath?:"", Utils.context!!,view)
            repository.startAgoraRecording()
        }catch (ex:Exception){
            Log.e(TAG, "startAudioVideoRecording: ${ex.message}")
        }
    }

    fun stopAudioVideoRecording(){
        try {
            ScreenViewRecorder.videoRecorder.stopPlaying()
            repository.stopAgoraClientCallRecording()
        }catch (e:Exception){
            Log.e(TAG, "stopAudioVideoRecording: ${e.message}")
        }
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
                uiState.updateUiState(state)
                processAfterUIUpdate(state.isReconnecting)
            }
        }
    }

    private fun processAfterUIUpdate(reconnecting: Boolean) {

        val voipState = PrefManager.getVoipState()

        when(uiState.recordingButtonState) {
            RecordingButtonState.SENTREQUEST -> {
                cancelRecordingTimer()
                if (uiState.isStartGameClicked) {
                    recordingStopButtonClickListener()
                }
            }
            RecordingButtonState.RECORDING -> {
                startRecordingTimer(this.uiState.recordingButtonState)
                val msg = Message.obtain().apply {
                    what = SHOW_RECORDING_PERMISSION_DIALOG
                }
                viewModelScope.launch(Dispatchers.Main) {
                    recordingLiveEvent.value = msg
                }

                recordingStartedUIChanges()
            }
            else -> {}
        }

        if (reconnecting) {
            this.uiState.currentState = "Reconnecting"
        } else if (uiState.isOnHold) {
            this.uiState.currentState = "Call on Hold"
        } else if (uiState.isRemoteUserMuted) {
            this.uiState.currentState = "User Muted the Call"
        } else {
            if (voipState == State.CONNECTED || voipState == State.RECONNECTING) {
                Log.d(TAG, "listenUIState: $")

                this.uiState.currentState = "Timer"
            }
        }

    }

    fun getTime(recordingButtonState: RecordingButtonState): Job? {
        try {
            timer = CoroutineScope(Dispatchers.IO).launch {
                delay(10000)
                if (recordingButtonState == RecordingButtonState.RECORDING) {
                    if (uiState.recordTime > 0) {
                        repository.stopCallRecording()
                        repository.startCallRecording()
                    }
                }
            }
        } catch (ex: Exception) {}
        return timer
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
                recordingWaitingForUserUI()
                repository.startCallRecording()
            }
            RecordingButtonState.SENTREQUEST -> {
                Log.i(TAG, "recordCall: cancelled")
                cancelRecording()
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
    }

    private fun stoppedRecUIchanges() {
        uiState.recordBtnTxt = "Record"
        uiState.recordBtnImg = R.drawable.call_fragment_record
        uiState.visibleCrdView = false
    }

    private fun recordingWaitingForUserUI() {
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

    fun cancelRecording(){
        repository.cancelRecordingRequest()
    }
    fun startGame(v:View){
        if (Utils.isInternetAvailable().not()) {
            Utils.showToast("Seems like you have no internet")
            return
        }
        CallAnalytics.addAnalytics(
            event = EventName.PLAY_GAME_CLICK,
            agoraCallId = PrefManager.getAgraCallId().toString(),
            agoraMentorId = PrefManager.getLocalUserAgoraId().toString()
        )
        repository.startGame()
    }

    fun endGame(v:View){
        if (Utils.isInternetAvailable().not()) {
            Utils.showToast("Wait for the internet to reconnect...")
            return
        }
        CallAnalytics.addAnalytics(
            event = EventName.END_GAME_CLICK,
            agoraCallId = PrefManager.getAgraCallId().toString(),
            agoraMentorId = PrefManager.getLocalUserAgoraId().toString()
        )
        cancelRecordingTimer()
        repository.endGame()
    }

    fun nextWord(v:View){
        CallAnalytics.addAnalytics(
            event = EventName.NEW_WORD_CLICK,
            agoraCallId = PrefManager.getAgraCallId().toString(),
            agoraMentorId = PrefManager.getLocalUserAgoraId().toString()
        )
        if (Utils.isInternetAvailable().not()) {
            Utils.showToast("Seems like you have no internet")
            return
        }
        cancelRecordingTimer()
        repository.nextGameWord()
        viewModelScope.launch(Dispatchers.Main) {
            Utils.showToast(toastText)
        }
    }

    fun cancelRecordingTimer(){
        if(timer?.isActive == true || timer?.isCompleted == true) {
            timer?.cancel()
            timer = null
        }
    }
    fun startRecordingTimer(state: RecordingButtonState){
        if(timer == null || timer?.isActive == false) {
           timer = getTime(state)
           timer?.start()
       }
    }
}