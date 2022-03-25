package com.joshtalks.joshskills.ui.voip.new_arch.ui.viewmodels

import android.view.View
import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import com.joshtalks.joshskills.base.BaseViewModel
import com.joshtalks.joshskills.ui.voip.new_arch.ui.models.CallData
import com.joshtalks.joshskills.ui.voip.new_arch.ui.models.CallType

class VoiceCallViewModel: BaseViewModel() {

    val isSpeakerOn = ObservableBoolean(false)
    val isMute = ObservableBoolean(false)
    val callStatus = ObservableField("outgoing")
    val callType = ObservableField("")

    fun initiateCall(v: View){
    }

    fun disconnectCall(v: View){
    }

    fun acceptCall(v: View){
    }

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

    fun observeCallStatus(v: View){
//        observe data and publsih status
       callStatus.set("ongoing")
    }
    fun getCallData():CallData{
        return CallDataObj
    }
}
object CallDataObj:CallData{
    override fun getProfileImage(): String? {
        return null
    }

    override fun getCallerName(): String {
       return "bhaskaru"
    }

    override fun getTopicHeader(): String {
        return "hey"
    }

    override fun getTopicName(): String {
        return "p2p test"
    }

    override fun getCallType():CallType {
        return CallType.NormalPracticePartner
    }

    override fun getCallTypeHeader(): String {
        return "FPP"
    }
}