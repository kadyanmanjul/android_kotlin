package com.joshtalks.joshskills.common.ui.voip.new_arch.ui.viewmodels

import android.app.Activity
import android.app.Application
import android.os.Message
import android.os.SystemClock
import android.util.Log
import android.view.View
import androidx.databinding.ObservableArrayList
import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import androidx.databinding.ObservableInt
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.joshtalks.joshskills.voip.base.constants.*
import com.joshtalks.joshskills.voip.base.log.Feature
import com.joshtalks.joshskills.voip.base.log.JoshLog
import com.joshtalks.joshskills.common.core.PrefManager as CorePrefManager
import com.joshtalks.joshskills.common.core.BLOCK_STATUS
//import com.joshtalks.joshskills.lesson.speaking.spf_models.BlockStatusModel
import com.joshtalks.joshskills.common.ui.voip.local.VoipPref
import com.joshtalks.joshskills.common.ui.voip.new_arch.ui.models.CallUIState
import com.joshtalks.joshskills.common.ui.voip.repository.RepositoryConstants
import com.joshtalks.joshskills.common.ui.voip.repository.WebrtcRepository
import com.joshtalks.joshskills.voip.Utils
import com.joshtalks.joshskills.voip.constant.*
import com.joshtalks.joshskills.voip.*
import com.joshtalks.joshskills.voip.data.ServiceEvents
import com.joshtalks.joshskills.voip.data.local.PrefManager
import com.joshtalks.joshskills.voip.voipanalytics.CallAnalytics
import com.joshtalks.joshskills.voip.voipanalytics.EventName
import kotlinx.coroutines.*
import java.util.ArrayDeque
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Duration

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
    private var singleLiveEvent = com.joshtalks.joshskills.common.base.EventLiveData
    var visibleCrdView = false
    var isListening = false
    var isPermissionGranted: ObservableBoolean = ObservableBoolean(false)
    var isExpertCallData = ObservableField(false)


    private val connectCallJob by lazy {
        viewModelScope.launch(start = CoroutineStart.LAZY) {
            mutex.withLock {
                if (PrefManager.getVoipState() == State.IDLE && isConnectionRequestSent.not()) {
                    Log.d(TAG, " connectCallJob : Inside - $callData  $callType")
                    repository.connectCall(callData,callType)
                    isConnectionRequestSent = true
                    if((callType==Category.FPP || callType==Category.EXPERT)&& source == FROM_ACTIVITY){
                        uiState.currentState = "Ringing..."
                    }
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
                    RepositoryConstants.CONNECTION_ESTABLISHED -> {
                        if (isListening.not()) {
                            isListening  = true
                            listenUIState()
                            listenVoipEvents()
                            if (isNotRestrictedFromCall())
                                connectCall()
                            else {
                                val msg = Message.obtain().apply {
                                    what = CLOSE_CALL_SCREEN
                                }
                                withContext(Dispatchers.Main) {
                                    singleLiveEvent.value = msg
                                }
                            }
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
                when (it.type) {
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
                            obj = it.data
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
                }
            }
        }
    }


    private fun listenUIState() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.observeUserDetails()?.collect { state ->
                Log.d(TAG, "listenUIState: $state")
                val voipState = PrefManager.getVoipState()
                Log.d(TAG, "listenUIState: State --> $voipState")
                if (uiState.startTime != state.startTime)
                    uiState.startTime = state.startTime

                if(voipState!=State.IDLE && voipState != State.SEARCHING) {
                    uiState.name = state.remoteUserName
                    uiState.profileImage = state.remoteUserImage ?: ""
                }
                try {
                    uiState.localUserName = Utils.context?.getMentorName()?:""
                    uiState.localUserProfile = Utils.context?.getMentorProfile()?:""
                }catch (ex:Exception){}

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
                    uiState.isSpeakerOn = state.isSpeakerOn
                }

                if (uiState.isMute != state.isOnMute) {
                    Log.d(TAG, "listenUIState: MUTE -- ${state.isOnMute}")
                    uiState.isMute = state.isOnMute
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
        val duration = SystemClock.elapsedRealtime() - VoipPref.getStartTimeStamp()
        if (duration < 2 * 60 * 1000) {
            CallAnalytics.addAnalytics(
                event = EventName.RED_BUTTON_PRESSED,
                agoraCallId = PrefManager.getAgraCallId().toString(),
                agoraMentorId = PrefManager.getLocalUserAgoraId().toString(),
                extra = PrefManager.getVoipState().name
            )
            val msg = Message.obtain().apply {
                what = SHOW_DISCONNECT_DIALOG
            }
            singleLiveEvent.value = msg
        }
        else {
            CallAnalytics.addAnalytics(
                event = EventName.DISCONNECTED_BY_RED_BUTTON,
                agoraCallId = PrefManager.getAgraCallId().toString(),
                agoraMentorId = PrefManager.getLocalUserAgoraId().toString(),
                extra = PrefManager.getVoipState().name
            )
            disconnect()
        }
    }

    fun disconnect() {
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


    private fun checkBlockStatusInSP(): Boolean {
        //TODO BlockStatusModel Model create globally so we can call this method

//        val blockStatus = CorePrefManager.getBlockStatusObject(BLOCK_STATUS)
//        if (blockStatus?.timestamp?.toInt() == 0 && blockStatus.duration == 0)
//            return false

        //TODO checkWithinBlockTimer() create model globally BlockStatusModel

//        if (checkWithinBlockTimer(blockStatus)) {
//            return true
//        } else {
//            CorePrefManager.putPrefObject(BLOCK_STATUS,
//                com.joshtalks.joshskills.lesson.speaking.spf_models.BlockStatusModel(0, 0, "", 0)
//            )
//            return false
//        }
        return false
    }

    //TODO checkWithinBlockTimer() create model globally BlockStatusModel
//    private fun checkWithinBlockTimer(blockStatus: com.joshtalks.joshskills.lesson.speaking.spf_models.BlockStatusModel?): Boolean {
//        if (blockStatus != null) {
//            val durationInMillis = Duration.ofMinutes(blockStatus.duration.toLong()).toMillis()
//            val unblockTimestamp = blockStatus.timestamp + durationInMillis
//            val currentTimestamp = System.currentTimeMillis()
//            if (currentTimestamp < unblockTimestamp) {
//                return true
//            }
//        }
//        return false
//    }

    private fun isNotRestrictedFromCall() : Boolean {
        return checkBlockStatusInSP().not() || callType == Category.EXPERT || callType == Category.FPP
    }
}