package com.joshtalks.joshskills.ui.voip.new_arch.ui.viewmodels

import android.graphics.Color
import android.util.Log
import android.view.View
import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import androidx.lifecycle.viewModelScope
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.base.BaseViewModel
import com.joshtalks.joshskills.core.TAG
import com.joshtalks.joshskills.core.showToast
import com.joshtalks.joshskills.ui.call.WebrtcRepository
import com.joshtalks.joshskills.ui.voip.new_arch.ui.callbar.CallBar
import com.joshtalks.joshskills.ui.voip.new_arch.ui.callbar.VoipPref
import com.joshtalks.joshskills.ui.voip.new_arch.ui.models.CallData
import com.joshtalks.joshskills.voip.calldetails.CallDetails
import com.joshtalks.joshskills.voip.constant.CALL_CONNECTED_EVENT
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class VoiceCallViewModel : BaseViewModel() {
    private var isConnectionRequestSent = false
    private val repository = WebrtcRepository()
    private val mutex = Mutex(false)
    private val callBar = CallBar()
    val isSpeakerOn = ObservableBoolean(false)
    val isMute = ObservableBoolean(false)
    val callStatus = ObservableField("outgoing")
    val callType = ObservableField("")
    val callStatusTest = ObservableField("Timer")
    val callData = HashMap<String, Any>()

    init { listenRepositoryEvents() }

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
                        voipLog?.log("CALL_CONNECTED_EVENT")
                    }
                    HOLD -> {
                        callStatusTest.set("Call on Hold")
                        voipLog?.log("HOLD")
                    }
                    UNHOLD -> {
                        callStatusTest.set("Timer")
                        voipLog?.log("UNHOLD")
                    }
                    MUTE -> {
                        callStatusTest.set("User Muted the Call")
                        voipLog?.log("Mute")
                    }
                    UNMUTE -> {
                        callStatusTest.set("Timer")
                        voipLog?.log("UNMUTE")
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
            repository.unmuteCall()
            isMute.set(false)
        } else {
            switchMicOff()
            repository.muteCall()
            isMute.set(true)
        }
    }

    private fun switchSpeakerOn(){
    }
    private fun switchSpeakerOff(){
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
            1 -> {
    //                 Normal Call
                "Practice with Partner"
            }
            2 -> {
    //                 FPP
                "Favorite Practice Partner"
            }
            3 -> {
    //                 Group Call
                "Group Call"
            }
            else -> ""
        }
    }

    override fun getStartTime(): Long {
       return VoipPref.getStartTimeStamp()
    }

    override fun getStartTime(): Long {
        return VoipPref.getStartTimeStamp()
    }
}