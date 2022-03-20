package com.joshtalks.joshskills.ui.voip.new_arch.ui.viewmodels

import android.view.View
import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import com.joshtalks.joshskills.base.BaseViewModel
import com.joshtalks.joshskills.ui.voip.new_arch.ui.models.CallData

class VoiceCallViewModel: BaseViewModel() {

    val isSpeakerOn = ObservableBoolean(false)
    val isMute = ObservableBoolean(false)
    val callStatus = ObservableField("")
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
//       set Callstatus here-> callStatus.set("")
    }
    fun getCallData():CallData{
        return CallDataObj
    }
}
object CallDataObj:CallData{
    override fun getProfileImage(): String {
        TODO("Not yet implemented")
    }

    override fun getCallerName(): String {
        TODO("Not yet implemented")
    }

    override fun getTopicHeader(): String {
        TODO("Not yet implemented")
    }

    override fun getTopicName(): String {
        TODO("Not yet implemented")
    }

    override fun getCallType(): com.joshtalks.joshskills.ui.voip.new_arch.ui.models.CallType {
        TODO("Not yet implemented")
    }

    override fun getCallTypeHeader(): String {
        TODO("Not yet implemented")
    }
}