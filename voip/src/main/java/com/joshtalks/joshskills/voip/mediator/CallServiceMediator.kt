package com.joshtalks.joshskills.voip.mediator

import kotlinx.coroutines.flow.SharedFlow

internal interface CallServiceMediator {
    fun observeEvents() : SharedFlow<Int>
    fun connectCall()
    fun switchAudio()
    fun disconnectCall()
}