package com.joshtalks.joshskills.premium.calling.mediator

import com.joshtalks.joshskills.premium.calling.constant.Category
import com.joshtalks.joshskills.premium.calling.constant.Event
import com.joshtalks.joshskills.premium.calling.data.ServiceEvents
import com.joshtalks.joshskills.premium.calling.data.UIState
import com.joshtalks.joshskills.premium.calling.webrtc.Envelope
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

// TODO: Need to Refactor
internal interface CallServiceMediator {
    // To Observer UI State
    fun observerUIState(): StateFlow<UIState>

    // To Observer UI Transition Events
    fun observerUITransition(): SharedFlow<ServiceEvents>

    // To Observer Mediator Events
    fun observeEvents(): SharedFlow<Envelope<Event>>

    // Needed for Connect Call
    fun connectCall(callCategory: Category, callData: HashMap<String, Any>)

    // Needed to show Incoming Call TODO: Need to check
    suspend fun handleIncomingCall(map: HashMap<String, String>)

    // Needed to hide Notification
    fun hideIncomingCall()
    fun declineIncomingCall()

    // Needed to receive User Action
    fun userAction(action: UserAction)

    // Used to destroy Mediator
    suspend fun onDestroy()

}
enum class UserAction {
    BACK_PRESS,
    DISCONNECT,
    RED_BUTTON_PRESSED,
    MUTE,
    UNMUTE,
    HOLD,
    UNHOLD,
    SPEAKER_ON,
    SPEAKER_OFF,
    TOPIC_IMAGE_CHANGE,
}