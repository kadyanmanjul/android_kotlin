package com.joshtalks.joshskills.ui.voip.new_arch.ui.viewmodels

import android.content.Intent
import android.util.Log
import android.view.View
import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import androidx.databinding.ObservableInt
import androidx.lifecycle.viewModelScope
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.base.BaseViewModel
import com.joshtalks.joshskills.base.constants.FPP
import com.joshtalks.joshskills.base.constants.GROUP
import com.joshtalks.joshskills.base.constants.PEER_TO_PEER
import com.joshtalks.joshskills.base.log.Feature
import com.joshtalks.joshskills.base.log.JoshLog
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.TAG
import com.joshtalks.joshskills.core.showToast
import com.joshtalks.joshskills.ui.call.WebrtcRepository
import com.joshtalks.joshskills.ui.voip.new_arch.ui.callbar.CallBar
import com.joshtalks.joshskills.ui.voip.new_arch.ui.callbar.VoipPref
import com.joshtalks.joshskills.ui.voip.new_arch.ui.models.CallData
import com.joshtalks.joshskills.ui.voip.new_arch.ui.views.CallFragment
import com.joshtalks.joshskills.ui.voip.new_arch.ui.views.VoiceCallActivity
import com.joshtalks.joshskills.voip.constant.CALL_CONNECTED_EVENT
import com.joshtalks.joshskills.voip.constant.CALL_DISCONNECT_REQUEST
import com.joshtalks.joshskills.voip.constant.CALL_INITIATED_EVENT
import com.joshtalks.joshskills.voip.constant.CONNECTED
import com.joshtalks.joshskills.voip.constant.ERROR
import com.joshtalks.joshskills.voip.constant.HOLD
import com.joshtalks.joshskills.voip.constant.IDLE
import com.joshtalks.joshskills.voip.constant.IPC_CONNECTION_ESTABLISHED
import com.joshtalks.joshskills.voip.constant.MUTE
import com.joshtalks.joshskills.voip.constant.RECONNECTED
import com.joshtalks.joshskills.voip.constant.RECONNECTING
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

const val CONNECTING = 1
const val ONGOING = 2

private const val TAG = "VoiceCallViewModel"
val voipLog = JoshLog.getInstanceIfEnable(Feature.VOIP)

class VoiceCallViewModel : BaseViewModel() {
    private var isConnectionRequestSent = false
    lateinit var source: String
    private val repository = WebrtcRepository()
    private val mutex = Mutex(false)
    private val callBar = CallBar()
    val isSpeakerOn = ObservableBoolean(false)
    val isMute = ObservableBoolean(false)
    val callType = ObservableField("")
    val callStatusTest = ObservableField("Connecting...")
    val callStatus = ObservableInt(getCallStatus())
    val callData = HashMap<String,Any>()
    private val connectCallJob by lazy {
        viewModelScope.launch(start = CoroutineStart.LAZY) {
            mutex.withLock {
                if (callBar.observerVoipState().value == IDLE && isConnectionRequestSent.not()) {
                    voipLog?.log("$callData")
                    repository.connectCall(callData)
                    isConnectionRequestSent = true
                }
            }
        }
    }

    init {
        listenUIState()
        listenRepositoryEvents()
    }

    private fun getCallStatus() : Int {
        val status = VoipPref.getVoipState()
        return if (status == CONNECTED) {
            ONGOING
        } else {
            CONNECTING
        }
    }

    private fun listenRepositoryEvents() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.observeRepositoryEvents().collect { it ->
                message.copyFrom(it)
                Log.d(TAG, "listenRepositoryEvents: ")
                voipLog?.log("observeCallEvents =-> $it")
                val data = when (message.what) {
                    CALL_INITIATED_EVENT -> {
                        voipLog?.log("CALL_INITIATED_EVENT")
                        "Connecting"
                    }
                    CALL_CONNECTED_EVENT -> {
                        callStatus.set(ONGOING)
                        voipLog?.log("CALL_CONNECTED_EVENT")
                        "Timer"
                    }
                    RECONNECTING -> {
                        voipLog?.log("RECONNECTING")
                        "Reconnecting"
                    }
                    RECONNECTED -> {
                        voipLog?.log("RECONNECTED")
                        "Timer"
                    }
                    CALL_DISCONNECT_REQUEST -> {
                        voipLog?.log("Call Disconnect")
                        showToast("Call Disconnect")
                        null
                    }
                    IPC_CONNECTION_ESTABLISHED -> {
                        connectCall()
                        null
                    }
                    else -> null
                }
                withContext(Dispatchers.Main) {
                    data?.let { callStatusTest.set(it) }
                    singleLiveEvent.value = message
                }
            }
        }
    }

    private fun listenUIState() {
        viewModelScope.launch(Dispatchers.IO) {
            callBar.observerVoipUIState().collect { uiState ->
                Log.d(TAG, "listenUIState: $uiState")
                if(uiState.isOnHold) {
                    callStatusTest.set("Call on Hold")
                    voipLog?.log("HOLD")
                } else if(uiState.isRemoteUserMuted) {
                    callStatusTest.set("User Muted the Call")
                    voipLog?.log("Mute")
                } else {
                    if(VoipPref.getVoipState()== CONNECTED)
                       callStatusTest.set("Timer")
                }

                if(uiState.isSpeakerOn) {
                    if(isSpeakerOn.get().not()) {
                        isSpeakerOn.set(true)
                        repository.turnOnSpeaker()
                    }
                } else {
                    if(isSpeakerOn.get()) {
                        isSpeakerOn.set(false)
                        repository.turnOffSpeaker()
                    }
                }

                if(uiState.isMute) {
                    if(isMute.get().not()) {
                        isMute.set(true)
                        repository.muteCall()
                    }
                } else {
                    if(isMute.get()) {
                        isMute.set(false)
                        repository.unmuteCall()
                    }
                }
            }
        }
    }

    private fun connectCall() {
        connectCallJob.start()
    }

    fun initiateCall(v: View) {}

    fun disconnectCall(v: View) {
        voipLog?.log("Disconnect Call")
        disconnect()
    }

    fun disconnect() {
        Log.d(TAG, "disconnect: ")
        repository.disconnectCall()
    }

    fun boundService() {
        voipLog?.log("binding Service")
        repository.startService()
    }

    fun unboundService() {
        voipLog?.log("unbound Service")
        repository.stopService()
    }

    fun switchSpeaker(v: View) {
        VoipPref.currentUserSpeakerState(isSpeakerOn.get().not())
    }

    fun switchMic(v: View) {
        VoipPref.currentUserMuteState(isMute.get().not())
    }

    fun getCallData(): CallData {
        return CallDataObj
    }
}

object CallDataObj : CallData {
    override fun getProfileImage(): String {
        return VoipPref.getProfileImage()
    }

    override fun getCallerName(): String {
        return VoipPref.getCallerName()
    }

    override fun getTopicName(): String {
        return VoipPref.getTopicName()
    }

    override fun getCallType(): Int {
        return VoipPref.getCallType()
    }

    override fun getCallTypeHeader(): String {
        return when(VoipPref.getCallType()) {
            PEER_TO_PEER -> "Practice with Partner"
            FPP -> "Favorite Practice Partner"
            GROUP -> "Group Call"
            else -> ""
        }
    }

    override fun getStartTime(): Long {
       return VoipPref.getStartTimeStamp()
    }

    val startActivity = fun(){
        val i = Intent(AppObjectController.joshApplication, VoiceCallActivity::class.java).apply {
            putExtra("openCallFragment",true)
        }
        AppObjectController.joshApplication.startActivity(i)
    }
}