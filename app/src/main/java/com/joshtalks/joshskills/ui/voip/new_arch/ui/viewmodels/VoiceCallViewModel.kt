package com.joshtalks.joshskills.ui.voip.new_arch.ui.viewmodels

import android.app.Activity
import android.app.AlertDialog
import android.app.Application
import android.content.DialogInterface
import android.os.Message
import android.util.Log
import android.view.LayoutInflater
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
import com.joshtalks.joshskills.core.showToast
import com.joshtalks.joshskills.ui.call.repository.RepositoryConstants.CONNECTION_ESTABLISHED
import com.joshtalks.joshskills.ui.call.repository.WebrtcRepository
import com.joshtalks.joshskills.ui.voip.new_arch.ui.models.CallUIState
import com.joshtalks.joshskills.util.CallRecording
import com.joshtalks.joshskills.voip.Utils
import com.joshtalks.joshskills.voip.constant.CALL_INITIATED_EVENT
import com.joshtalks.joshskills.voip.constant.CANCEL_INCOMING_TIMER
import com.joshtalks.joshskills.voip.constant.CLOSE_CALL_SCREEN
import com.joshtalks.joshskills.voip.constant.RECONNECTING_FAILED
import com.joshtalks.joshskills.voip.constant.State
import com.joshtalks.joshskills.voip.data.ServiceEvents
import com.joshtalks.joshskills.voip.data.local.PrefManager
import com.joshtalks.joshskills.voip.getTempFileForCallRecording
import com.joshtalks.joshskills.voip.voipanalytics.CallAnalytics
import com.joshtalks.joshskills.voip.voipanalytics.EventName
import kotlinx.android.synthetic.main.fragment_call.view.*
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

const val CONNECTING = 1
const val ONGOING = 2

val voipLog = JoshLog.getInstanceIfEnable(Feature.VOIP)

class VoiceCallViewModel(application: Application) : AndroidViewModel(application) {
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
    var recordFile: File? = null
    var visibleCrdView = false

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

    fun showAlertDialogBox(v: View) {
        val builder: AlertDialog.Builder =
            AlertDialog.Builder(v.context)

        val customLayout: View = LayoutInflater.from(v.context)
            .inflate(R.layout.dialog_record_call, null)

        builder.setView(customLayout)

        builder.setPositiveButton("ACCEPT") { p0, p1 ->
            // TODO: ACCEPTED
        }

        builder.setNegativeButton("DECLINE") { p0, p1 ->
            // TODO: DECLINED
        }

        val dialog: AlertDialog = builder.create()
        dialog.show()
    }

    fun showRecordCallCardview(v: View) {
        Log.i("RECORD CALL: ", "started!")
        when(recordCnclStop) {
            1-> {
                uiState.visibleCrdView = false
                recordCnclStop = 0
                uiState.recordBtnImg = R.drawable.call_fragment_record
                uiState.recordBtnTxt = "Record"
            }
            0 -> {
                uiState.visibleCrdView = true
                recordCnclStop = 1
                uiState.recordBtnImg = R.drawable.cancel_record
                uiState.recordBtnTxt = "Cancel"
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
                        listenUIState()
                        listenVoipEvents()
                        connectCall()
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
                            what = if(source == FROM_INCOMING_CALL)
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
                    }
                    ServiceEvents.CLOSE_CALL_SCREEN -> {
                        val msg = Message.obtain().apply {
                            what = CLOSE_CALL_SCREEN
                        }
                        withContext(Dispatchers.Main) {
                            singleLiveEvent.value = msg
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
                    ServiceEvents.START_RECORDING -> {
                        updateRecordingUI()
                    }
                    ServiceEvents.STOP_RECORDING -> {
                        stopRecording()
                        updateRecordingUI()
                    }
                    ServiceEvents.CALL_RECORDING_ACCEPT -> {
                        startRecording()
                        updateRecordingUI()
                    }
                    ServiceEvents.CALL_RECORDING_REJECT -> {
                        showToast("User declined the request to record")
                    }
                }
            }
        }
    }

    private fun updateRecordingUI() {
            // TODO either remove this function and change the observable parameters
    }

    private fun startRecording() {
        Utils.context?.getTempFileForCallRecording()?.let { file ->
            recordFile = file
            CallRecording.audioRecording.startPlayer(recordFile, Utils.context!!)
        }
    }

    fun stopRecording() {
        CallRecording.audioRecording.stopPlaying()
        //TODO shave the file path to db and send to server 'recordFile'
        Log.d(TAG, "stopRecording() called  $recordFile")
    }

    private fun listenUIState() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.observeUserDetails()?.collect { state ->
                Log.d(TAG, "listenUIState: $state")
                val voipState = PrefManager.getVoipState()
                Log.d(TAG, "listenUIState: State --> $voipState")
                if (uiState.startTime != state.startTime)
                    uiState.startTime = state.startTime
                uiState.name = state.remoteUserName
                uiState.profileImage = state.remoteUserImage ?: ""
                uiState.topic = state.topicName
                uiState.topicImage = state.currentTopicImage
                uiState.type = state.callType
                uiState.occupation = getOccupationText(state.aspiration,state.occupation)
                uiState.title = when (state.callType) {
                    PEER_TO_PEER -> "Practice with Partner"
                    FPP -> "Favorite Practice Partner"
                    GROUP -> "Group Call"
                    else -> ""
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

    private fun checkIfNullOrEmpty(word : String) : Boolean{
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
        }
        else {
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
        val isRecordingInitiated = uiState.isRecording
        uiState.isRecording = isRecordingInitiated.not()
        if (isRecordingInitiated) {
            CallAnalytics.addAnalytics(
                event = EventName.RECORDING_STOPPED,
                agoraCallId = PrefManager.getAgraCallId().toString(),
                agoraMentorId = PrefManager.getLocalUserAgoraId().toString()
            )
            repository.stopCallRecording()
        }
        else {
            CallAnalytics.addAnalytics(
                event = EventName.RECORDING_INITIATED,
                agoraCallId = PrefManager.getAgraCallId().toString(),
                agoraMentorId = PrefManager.getLocalUserAgoraId().toString()
            )
            repository.startCallRecording()
        }
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
        }
        else {
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

    fun getNewTopicImage(v:View){
        if(Utils.isInternetAvailable().not()){
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
}