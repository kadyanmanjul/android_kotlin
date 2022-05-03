package com.joshtalks.joshskills.ui.voip.new_arch.ui.viewmodels

import android.app.Activity
import android.os.Message
import android.util.Log
import android.view.View
import androidx.databinding.ObservableField
import androidx.databinding.ObservableInt
import androidx.lifecycle.viewModelScope
import com.joshtalks.joshskills.base.BaseViewModel
import com.joshtalks.joshskills.base.constants.FPP
import com.joshtalks.joshskills.base.constants.GROUP
import com.joshtalks.joshskills.base.constants.PEER_TO_PEER
import com.joshtalks.joshskills.base.log.Feature
import com.joshtalks.joshskills.base.log.JoshLog
import com.joshtalks.joshskills.core.TAG
import com.joshtalks.joshskills.ui.call.repository.RepositoryConstants.*
import com.joshtalks.joshskills.ui.call.repository.WebrtcRepository
import com.joshtalks.joshskills.ui.voip.new_arch.ui.models.CallUIState
import com.joshtalks.joshskills.voip.constant.*
import com.joshtalks.joshskills.voip.data.ServiceEvents
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.*
import kotlin.collections.HashMap

const val CONNECTING = 1
const val ONGOING = 2

private const val TAG = "VoiceCallViewModel"
val voipLog = JoshLog.getInstanceIfEnable(Feature.VOIP)

class VoiceCallViewModel : BaseViewModel() {
    private var isConnectionRequestSent = false
    lateinit var source: String
    private val repository = WebrtcRepository(viewModelScope)
    private val mutex = Mutex(false)
    val callType = ObservableField("")
    val callStatus = ObservableInt(getCallStatus())
    val callData = HashMap<String, Any>()
    val uiState by lazy { CallUIState() }
    val pendingEvents = ArrayDeque<Int>()

    private val connectCallJob by lazy {
        viewModelScope.launch(start = CoroutineStart.LAZY) {
            mutex.withLock {
                Log.d(TAG, "connectCallJob : Out ${repository.getVoipState()}")
                Log.d(TAG, "connectCallJob : Out ${isConnectionRequestSent.not()}")
                if ((repository.getVoipState() == IDLE || repository.getVoipState() == LEAVING) && isConnectionRequestSent.not()) {
                    Log.d(TAG, " connectCallJob : Inside")
                    voipLog?.log("$callData")
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
                            what = CALL_INITIATED_EVENT
                        }
                        withContext(Dispatchers.Main) {
                            singleLiveEvent.value = msg
                        }
                    }
                    ServiceEvents.CALL_CONNECTED_EVENT -> {
                        uiState.currentState = "Timer"
                    }
                    ServiceEvents.CALL_DISCONNECT_REQUEST -> {
                        val msg = Message.obtain().apply {
                            what = CALL_DISCONNECT_REQUEST
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
        Log.d(TAG, "listenUIState: ")
        viewModelScope.launch(Dispatchers.IO) {
            repository.observeUserDetails()?.collect { state ->
                Log.d(TAG, "listenUIState: $state")
                if (uiState.startTime != state.startTime)
                    uiState.startTime = state.startTime
                uiState.name = state.remoteUserName
                uiState.profileImage = state.remoteUserImage ?: ""
                uiState.topic = state.topicName
                uiState.type = state.callType
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
                    if (repository.getVoipState() == CONNECTED)
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

    private fun getCallStatus(): Int {
        val status = repository.getVoipState()
        return if (status == CONNECTED) {
            ONGOING
        } else {
            CONNECTING
        }
    }

    private fun connectCall() {
        connectCallJob.start()
    }

    // User Action
    fun disconnectCall(v: View) {
        voipLog?.log("Disconnect Call")
//        CallAnalytics.addAnalytics(
//            event = EventName.DISCONNECTED_BY_RED_BUTTON,
//            agoraMentorId =  VoipPref.getCurrentUserAgoraId().toString(),
//            agoraCallId = VoipPref.getCurrentCallId().toString()
//        )
        disconnect()
    }

    fun disconnect() {
        Log.d(TAG, "disconnect: ")
        repository.disconnectCall()
        val msg = Message.obtain().apply {
            what = CALL_DISCONNECT_REQUEST
        }
        singleLiveEvent.value = msg
    }

    // User Action
    fun switchSpeaker(v: View) {
        val isOnSpeaker = uiState.isSpeakerOn
        uiState.isSpeakerOn = isOnSpeaker.not()
        if (isOnSpeaker)
            repository.turnOffSpeaker()
        else {
//            CallAnalytics.addAnalytics(
//                event = EventName.SPEAKER_ON,
//                agoraMentorId =  VoipPref.getCurrentUserAgoraId().toString(),
//                agoraCallId = VoipPref.getCurrentCallId().toString()
//            )

            repository.turnOnSpeaker()
        }
    }

    // User Action
    fun switchMic(v: View) {
        val isOnMute = uiState.isMute
        uiState.isMute = isOnMute.not()
        if (isOnMute) {
            repository.unmuteCall()
        }
        else {
//            CallAnalytics.addAnalytics(
//                event = EventName.MIC_ON,
//                agoraMentorId =  VoipPref.getCurrentUserAgoraId().toString(),
//                agoraCallId = VoipPref.getCurrentCallId().toString()
//            )
            repository.muteCall()
        }
    }

    // User Action
    fun backPress() {}

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