package com.joshtalks.joshskills.di

import com.joshtalks.joshskills.base.core.Envelope
import com.joshtalks.joshskills.di.annotation.AppScope
import kotlinx.coroutines.channels.Channel
import javax.inject.Inject

@AppScope
class ApplicationEventListener @Inject constructor() {
    private val channel = Channel<Envelope<ApplicationEvent>>(Channel.UNLIMITED)

    suspend fun emitEvent(event : Envelope<ApplicationEvent>) {
        channel.send(event)
    }

    fun observerGlobalEvent() : Channel<Envelope<ApplicationEvent>> {
        return channel
    }
}

suspend inline fun<T> Channel<T>.handleAllEvent(block : (T)->Unit) {
    while (true) {
        val event = this.receive()
        block(event)
    }
}

enum class ApplicationEvent {
    UNAUTHORISED,
    LOGOUT,
    CLEAR_DATA
}