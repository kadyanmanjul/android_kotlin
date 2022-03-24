package com.joshtalks.joshskills.voip.mediator

import kotlinx.coroutines.flow.SharedFlow

internal interface CallServiceMediator {
    fun observeEvents() : SharedFlow<Int>
    fun connectCall(callType: CallType)
    fun switchAudio()
    fun disconnectCall()
}

enum class CallType {
    PEER_TO_PEER,
    FPP,
    GROUP
}