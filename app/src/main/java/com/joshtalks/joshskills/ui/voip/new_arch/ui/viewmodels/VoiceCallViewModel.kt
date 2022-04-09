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
import com.joshtalks.joshskills.voip.constant.SWITCHED_TO_BLUETOOTH
import com.joshtalks.joshskills.voip.constant.SWITCHED_TO_HANDSET
import com.joshtalks.joshskills.voip.constant.SWITCHED_TO_SPEAKER
import com.joshtalks.joshskills.voip.constant.SWITCHED_TO_WIRED
import com.joshtalks.joshskills.voip.constant.UNHOLD
import com.joshtalks.joshskills.voip.constant.UNMUTE
import com.joshtalks.joshskills.voip.voipLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
const val CONNECTING = 1
const val ONGOING = 2


class VoiceCallViewModel : BaseViewModel() {
    private var isConnectionRequestSent = false
    private val repository = WebrtcRepository()
    private val mutex = Mutex(false)
    private val callBar = CallBar()
    val isSpeakerOn = ObservableBoolean(false)
    val isMute = ObservableBoolean(false)
    val callType = ObservableField("")
    val callStatusTest = ObservableField("Connecting...")
    val callStatus = ObservableInt(getCallStatus())
    val callData = HashMap<String,Any>()

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
            repository.observeRepositoryEvents().collect {
                message.copyFrom(it)
                voipLog?.log("observeCallEvents =-> $it")
                when (message.what) {
                    CALL_INITIATED_EVENT -> {
                        voipLog?.log("CALL_INITIATED_EVENT")
                    }
                    CALL_CONNECTED_EVENT -> {
                        callStatusTest.set("Timer")
                        callStatus.set(ONGOING)
                        voipLog?.log("CALL_CONNECTED_EVENT")
                    }
                    RECONNECTING -> {
                        callStatusTest.set("Reconnecting")
                        voipLog?.log("RECONNECTING")
                    }
                    RECONNECTED -> {
                        callStatusTest.set("Timer")
                        voipLog?.log("RECONNECTED")
                    }
                    CALL_DISCONNECT_REQUEST -> {
                        voipLog?.log("Call Disconnect")
                        showToast("Call Disconnect")
                    }
                    IPC_CONNECTION_ESTABLISHED -> {
                        connectCall()
                    }
                }
                withContext(Dispatchers.Main) {
                    singleLiveEvent.value = message
                }
            }
        }
    }

    private fun listenUIState() {
        viewModelScope.launch(Dispatchers.IO) {
            callBar.observerVoipUIState().collectLatest { uiState ->
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
                    isSpeakerOn.set(true)
                    repository.turnOnSpeaker()
                } else {
                    isSpeakerOn.set(false)
                    repository.turnOffSpeaker()
                }

                if(uiState.isMute) {
                    isMute.set(true)
                    repository.muteCall()
                } else {
                    isMute.set(false)
                    repository.unmuteCall()
                }
            }
        }
    }

    private suspend fun connectCall() {
        mutex.withLock {
            if (callBar.observerVoipState().value == IDLE && isConnectionRequestSent.not()) {
                voipLog?.log("$callData")
                repository.connectCall(callData)
                isConnectionRequestSent = true
            }
        }
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