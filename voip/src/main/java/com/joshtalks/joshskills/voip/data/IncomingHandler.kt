package com.joshtalks.joshskills.voip.data

import android.os.Handler
import android.os.Looper
import android.os.Message
import android.os.Messenger
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

internal class IncomingHandler private constructor(val scope : CoroutineScope, val events : SharedFlow<Int>) : Handler(
    Looper.getMainLooper()) {
    private val TAG = "IncomingHandler"
    private var clientMessageChannel: Messenger? = null

    init {
        scope.launch {
            events.collect {
                Log.d(TAG, " Events form Service to Handler : $it")
                clientMessageChannel?.send(Message().apply {
                    what = it
                })
            }
        }
    }

    companion object {
        @Volatile private lateinit var INSTANCE: IncomingHandler
        fun getInstance(scope : CoroutineScope, events : SharedFlow<Int>) : IncomingHandler {
            if (this::INSTANCE.isInitialized)
                return INSTANCE
            else
                synchronized(this) {
                    return if (this::INSTANCE.isInitialized)
                        INSTANCE
                    else {
                        IncomingHandler(scope, events)
                    }
                }
        }
    }

    // User to send Message from Incoming Handler to Service
    private val flowEvent by lazy {
        MutableSharedFlow<Int>(replay = 0)
    }

    // Service can get Flow to Listen for Messages
    fun observerFlow() : SharedFlow<Int> {
        return flowEvent
    }

    // Handling Message coming from Service
    override fun handleMessage(msg: Message) {
        sendMessage(msg.what)
        clientMessageChannel = msg.replyTo
    }

    // Sending Message to Service
    private fun sendMessage(msg : Int) {
        scope.launch {
            flowEvent.emit(msg)
        }
    }
}

//private data class ServerHandler(val handler: Handler, val flow : Flow)