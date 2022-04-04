package com.joshtalks.joshskills.ui.voip.new_arch.ui.viewmodels

import android.os.Message
import android.view.View
import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import androidx.lifecycle.viewModelScope
import com.joshtalks.joshskills.base.BaseViewModel
import com.joshtalks.joshskills.core.showToast
import com.joshtalks.joshskills.ui.call.WebrtcRepository
import com.joshtalks.joshskills.ui.voip.new_arch.ui.callbar.CallBar
import com.joshtalks.joshskills.ui.voip.new_arch.ui.models.CallData
import com.joshtalks.joshskills.ui.voip.new_arch.ui.models.CallType
import com.joshtalks.joshskills.voip.communication.constants.CLOSE_CALLING_FRAGMENT
import com.joshtalks.joshskills.voip.constant.CALL_CONNECTED_EVENT
import com.joshtalks.joshskills.voip.constant.CALL_DISCONNECTED
import com.joshtalks.joshskills.voip.constant.CALL_DISCONNECTING_EVENT
import com.joshtalks.joshskills.voip.constant.CALL_DISCONNECT_REQUEST
import com.joshtalks.joshskills.voip.constant.CALL_INITIATED_EVENT
import com.joshtalks.joshskills.voip.constant.ERROR
import com.joshtalks.joshskills.voip.constant.HOLD
import com.joshtalks.joshskills.voip.constant.IDLE
import com.joshtalks.joshskills.voip.constant.IPC_CONNECTION_ESTABLISHED
import com.joshtalks.joshskills.voip.constant.MUTE
import com.joshtalks.joshskills.voip.constant.RECONNECTED
import com.joshtalks.joshskills.voip.constant.RECONNECTING
import com.joshtalks.joshskills.voip.constant.UNHOLD
import com.joshtalks.joshskills.voip.constant.UNMUTE
import com.joshtalks.joshskills.voip.voipLog
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class VoiceCallViewModel : BaseViewModel() {
    private var isConnectionRequestSent = false
    private val repository = WebrtcRepository()
    private val mutex = Mutex(false)
    private val callBar = CallBar()
    val isSpeakerOn = ObservableBoolean(false)
    val isMute = ObservableBoolean(false)
    val callStatus = ObservableField("outgoing")
    val callType = ObservableField("")
    val callStatusTest = ObservableField("")
    val callData = HashMap<String, Any>()

    init { listenRepositoryEvents() }

    private fun listenRepositoryEvents() {
        viewModelScope.launch {
            repository.observeRepositoryEvents().collect {
                message.copyFrom(it)
                voipLog?.log("observeCallEvents =-> $it")
                when (message.what) {
                    CALL_INITIATED_EVENT -> {
                        voipLog?.log("CALL_INITIATED_EVENT")
                    }
                    CALL_CONNECTED_EVENT -> {
                        callStatusTest.set("Call Connected")
                        voipLog?.log("CALL_CONNECTED_EVENT")
                    }
                    HOLD -> {
                        callStatusTest.set("Call on Hold")
                        voipLog?.log("HOLD")
                    }
                    UNHOLD -> {
                        callStatusTest.set("")
                        voipLog?.log("UNHOLD")
                    }
                    MUTE -> {
                        callStatusTest.set("User Muted the Call")
                        voipLog?.log("Mute")
                    }
                    UNMUTE -> {
                        callStatusTest.set("")
                        voipLog?.log("UNMUTE")
                    }
                    RECONNECTING -> {
                        callStatusTest.set("Reconnecting")
                        voipLog?.log("RECONNECTING")
                    }
                    RECONNECTED -> {
                        callStatusTest.set("")
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
                singleLiveEvent.value = message
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

    fun acceptCall(v: View) {}

    fun switchSpeaker(v: View) {
        if (isSpeakerOn.get()) {
            switchSpeakerOff()
            isSpeakerOn.set(false)
        } else {
            switchSpeakerOn()
            isSpeakerOn.set(true)
        }
    }

    fun switchMic(v: View) {
        if (isMute.get()) {
            switchMicOn()
            isMute.set(false)
        } else {
            switchMicOff()
            isMute.set(true)
        }
    }

    private fun switchSpeakerOn() {
    }

    private fun switchSpeakerOff() {
    }

    private fun switchMicOn() {
    }

    private fun switchMicOff() {
    }

    fun observeCallStatus(v: View) {
//        observe data and publsih status
        callStatus.set("ongoing")
    }

    fun getCallData(): CallData {
        return CallDataObj
    }
}

object CallDataObj : CallData {
    override fun getProfileImage(): String? {
        return null
    }

    override fun getCallerName(): String {
        return "Testing Name"
    }

    override fun getTopicHeader(): String {
        return "Testing"
    }

    override fun getTopicName(): String {
        return "p2p test"
    }

    override fun getCallType(): CallType {
        return CallType.NormalPracticePartner
    }

    override fun getCallTypeHeader(): String {
        return "P2P"
    }
}