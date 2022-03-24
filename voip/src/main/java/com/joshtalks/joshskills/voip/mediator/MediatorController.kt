package com.joshtalks.joshskills.voip.mediator

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

internal class MediatorController : CallServiceMediator {
    lateinit var currentMediator : CallServiceMediator
    private set

    private val flow by lazy {
        MutableSharedFlow<Int>(replay = 0)
    }

    fun getMediator() {

    }

    override fun observeEvents(): SharedFlow<Int> {
        TODO("Not yet implemented")
    }

    override fun connectCall() {
        TODO("Not yet implemented")
    }

    override fun switchAudio() {
        TODO("Not yet implemented")
    }

    override fun disconnectCall() {
        TODO("Not yet implemented")
    }
}