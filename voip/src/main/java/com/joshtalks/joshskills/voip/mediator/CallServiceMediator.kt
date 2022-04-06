package com.joshtalks.joshskills.voip.mediator

import com.joshtalks.joshskills.voip.communication.model.IncomingCall
import com.joshtalks.joshskills.voip.communication.model.OutgoingData
import kotlinx.coroutines.flow.SharedFlow

internal interface CallServiceMediator {
    fun observeEvents() : SharedFlow<Int>
    fun observeState() : SharedFlow<Int>
    fun connectCall(callType: Int, callData : HashMap<String, Any>)
    fun muteAudioStream(muteAudio : Boolean)
    fun sendEventToServer(data : OutgoingData)
    fun showIncomingCall(incomingCall : IncomingCall)
    fun switchAudio()
    fun disconnectCall()
}