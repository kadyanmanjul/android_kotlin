package com.joshtalks.joshskills.voip.mediator

import com.joshtalks.joshskills.voip.communication.PubnubState
import com.joshtalks.joshskills.voip.communication.model.IncomingCall
import com.joshtalks.joshskills.voip.constant.Event
import com.joshtalks.joshskills.voip.data.ServiceEvents
import com.joshtalks.joshskills.voip.data.UIState
import com.joshtalks.joshskills.voip.webrtc.Envelope
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

// TODO: Need to Refactor
internal interface CallServiceMediator {
    // To Observer UI State
    fun observerUIState() : StateFlow<UIState>
    // To Observer UI Transition Events
    fun observerUITransition() : SharedFlow<ServiceEvents>
    // To Observer Mediator Events
    fun observeEvents() : SharedFlow<Envelope<Event>>
    // Needed for Connect Call
    fun connectCall(callType: Int, callData : HashMap<String, Any>)
    // Needed to show Incoming Call TODO: Need to check
    fun showIncomingCall(incomingCall : IncomingCall)
    // Needed to hide Notification
    fun hideIncomingCall()
    // Needed to receive User Action
    fun userAction(action: UserAction)
    // Used to destroy Mediator
    fun onDestroy()
}

enum class UserAction {
    BACK_PRESS,
    DISCONNECT,
    MUTE,
    UNMUTE,
    HOLD,
    UNHOLD,
    SPEAKER_ON,
    SPEAKER_OFF,
    TOPIC_IMAGE_CHANGE,
}