package com.joshtalks.joshskills.voip.data

import android.app.Service
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.os.Messenger
import android.util.Log
import com.joshtalks.joshskills.voip.Utils
import com.joshtalks.joshskills.voip.mediator.PeerToPeerMediator
import java.util.concurrent.Flow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class WebrtcService : Service() {
    private val TAG = "WebrtcService"
    private val serviceToHandlerFlow = MutableSharedFlow<Int>()
    private val serviceToMediatorFlow = MutableSharedFlow<Int>()
    private val mediator by lazy { PeerToPeerMediator(serviceToMediatorFlow) }
    private val scope by lazy { CoroutineScope(Dispatchers.IO) }

    override fun onCreate() {
        super.onCreate()
        scope.launch {
            mediator.observeEvents().collect {
                sendEventToClient(it)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onBind(intent: Intent?): IBinder? {
        val handler = IncomingHandler.getInstance(scope, serviceToHandlerFlow)
        val messenger = Messenger(handler)
        observeHandlerEvents(handler)
        return messenger.binder
    }

    private fun observeHandlerEvents(handler: IncomingHandler) {
        scope.launch {
            handler.observerFlow().collect {
                Log.d(TAG, "observeHandlerEvents: $it")
            }
        }
    }

    private fun sendEventToClient(data : Int) {
        Log.d(TAG, "sendEventToClient: $data")
        scope.launch {
            serviceToHandlerFlow.emit(data)
        }
    }
}