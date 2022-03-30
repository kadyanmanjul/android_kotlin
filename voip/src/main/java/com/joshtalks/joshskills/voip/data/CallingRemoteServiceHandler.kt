package com.joshtalks.joshskills.voip.data

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.os.Messenger
import android.util.Log
import com.joshtalks.joshskills.base.constants.INTENT_DATA_CONNECT_CALL
import com.joshtalks.joshskills.voip.constant.CALL_CONNECT_REQUEST
import com.joshtalks.joshskills.voip.constant.IPC_CONNECTION_ESTABLISHED
import com.joshtalks.joshskills.voip.voipLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

internal class CallingRemoteServiceHandler private constructor(val scope : CoroutineScope) : Handler(
    Looper.getMainLooper()) {
    private var clientMessageChannel: Messenger? = null

    fun sendMessageToRepository(message: Int) {
        voipLog?.log("Events form Service to Handler : $message")
        val msg = Message()
        msg.what = message
        clientMessageChannel?.send(msg)
    }

    companion object {
        @Volatile private lateinit var INSTANCE: CallingRemoteServiceHandler
        fun getInstance(scope : CoroutineScope) : CallingRemoteServiceHandler {
            if (this::INSTANCE.isInitialized)
                return INSTANCE
            else
                synchronized(this) {
                    return if (this::INSTANCE.isInitialized)
                        INSTANCE
                    else {
                        CallingRemoteServiceHandler(scope)
                    }
                }
        }
    }

    // User to send Message from Incoming Handler to Service
    private val flowEvent by lazy {
        MutableSharedFlow<Message>(replay = 0)
    }

    // Service can get Flow to Listen for Messages
    fun observerFlow() : SharedFlow<Message> {
        return flowEvent
    }

    // Handling Message coming from Service
    override fun handleMessage(msg: Message) {
        voipLog?.log("${msg}")
        if(msg.what == IPC_CONNECTION_ESTABLISHED)
            clientMessageChannel = msg.replyTo
        else
            sendMessageToService(msg)
    }

    // Sending Message to Service
    private fun sendMessageToService(msg : Message) {
        val data = Message()
        data.copyFrom(msg)
        voipLog?.log("${msg}")
        if(data.what == CALL_CONNECT_REQUEST) {
            val bundle = data.obj as? Bundle
            data.obj = bundle?.getSerializable(INTENT_DATA_CONNECT_CALL) as? HashMap<String, Any>
        }
        scope.launch {
            flowEvent.emit(data)
        }
    }
}