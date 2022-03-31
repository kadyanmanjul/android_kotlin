package com.joshtalks.joshskills.ui.voip.new_arch.ui.viewmodels

import android.os.Message
import android.view.View
import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import androidx.lifecycle.viewModelScope
import com.joshtalks.joshskills.base.BaseViewModel
import com.joshtalks.joshskills.core.showToast
import com.joshtalks.joshskills.ui.call.WebrtcRepository
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
import com.joshtalks.joshskills.voip.constant.IPC_CONNECTION_ESTABLISHED
import com.joshtalks.joshskills.voip.voipLog
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class VoiceCallViewModel : BaseViewModel() {
    val isSpeakerOn = ObservableBoolean(false)
    val isMute = ObservableBoolean(false)
    val callStatus = ObservableField("outgoing")
    val callType = ObservableField("")
    val callStatusTest = ObservableField("")
    private val repository = WebrtcRepository()
    val callData = HashMap<String, Any>()
    val mutex = Mutex(true)

    init {
        voipLog?.log("Before scope")
        viewModelScope.launch {
            repository.observeCallEvents().collect {
                message.copyFrom(it)
                voipLog?.log("observeCallEvents =-> $it")
                when(it.what) {
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
                    ERROR -> {
                        voipLog?.log("Error Occurred")
                        showToast("Error Occoured")
                    }
                    CALL_DISCONNECT_REQUEST -> {
                        voipLog?.log("Call Disconnect")
                        showToast("Call Disconnect")
                    }
                }
                singleLiveEvent.value = message
            }
        }
        viewModelScope.launch {
            // TODO: Handle the case when it will rebind
            repository.getRepositoryEvents().collect {
                voipLog?.log("getRepository --> $it")
                when(it) {
                    IPC_CONNECTION_ESTABLISHED -> {
                        try {
                            mutex.unlock()
                        } catch (e : Exception) {
                            e.printStackTrace()
                            voipLog?.log("Mutex Unlock Error")
                        }
                    }
                }
            }
        }
    }

    fun initiateCall(v: View){}

    fun disconnectCall(v: View) {
        voipLog?.log("Disconnect Call")
        repository.disconnectCall()
    }

    fun connectCall() {
        viewModelScope.launch {
            mutex.withLock {
                voipLog?.log("$callData")
                repository.connectCall(callData)
            }
        }
    }

    fun boundService() {
        voipLog?.log("binding Service")
        repository.startService()
    }

    fun unboundService() {
        voipLog?.log("unbound Service")
        repository.stopService()
    }

    fun acceptCall(v: View){}

    fun switchSpeaker(v: View){
        if(isSpeakerOn.get()){
            switchSpeakerOff()
            isSpeakerOn.set(false)
        }else{
            switchSpeakerOn()
            isSpeakerOn.set(true)
        }
    }

    fun switchMic(v: View){
        if(isMute.get()){
            switchMicOn()
            isMute.set(false)
        }else{
            switchMicOff()
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

    fun getCallData():CallData {
        return CallDataObj
    }
}

object CallDataObj:CallData{
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

    override fun getCallType():CallType {
        return CallType.NormalPracticePartner
    }

    override fun getCallTypeHeader(): String {
        return "P2P"
    }
}