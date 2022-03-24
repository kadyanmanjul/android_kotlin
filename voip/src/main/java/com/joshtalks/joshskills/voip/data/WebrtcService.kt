package com.joshtalks.joshskills.voip.data

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.os.Messenger
import com.joshtalks.joshskills.voip.R
import com.joshtalks.joshskills.voip.Utils
import com.joshtalks.joshskills.voip.mediator.CallType
import com.joshtalks.joshskills.voip.mediator.CallingMediator
import com.joshtalks.joshskills.voip.mediator.PeerToPeerCalling
import com.joshtalks.joshskills.voip.voipLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class WebrtcService : Service() {
    private val serviceToHandlerFlow = MutableSharedFlow<Int>()
    private val serviceToMediatorFlow = MutableSharedFlow<Int>()
    private val mediator by lazy { CallingMediator(serviceToMediatorFlow) }
    private val scope by lazy { CoroutineScope(Dispatchers.IO) }

    override fun onCreate() {
        super.onCreate()
        voipLog?.log("Creating Service")
        showNotification()
        hideNotification()
        scope.launch {
            mediator.observeEvents().collect {
                sendEventToClient(it)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        voipLog?.log("StartService --- OnStartCommand")
        Utils.apiToken = intent?.extras?.getString("token")
        Utils.uuid = intent?.extras?.getString("mentor")
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onBind(intent: Intent?): IBinder? {
        voipLog?.log("Binding ....")
        val handler = IncomingHandler.getInstance(scope, serviceToHandlerFlow)
        val messenger = Messenger(handler)
        observeHandlerEvents(handler)
        return messenger.binder
    }

    private fun observeHandlerEvents(handler: IncomingHandler) {
        scope.launch {
            handler.observerFlow().collect {
                voipLog?.log("observeHandlerEvents: $it")
            }
        }
    }

    private fun sendEventToClient(data : Int) {
        voipLog?.log("sendEventToClient: $data")
        scope.launch {
            serviceToHandlerFlow.emit(data)
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        showNotification()
        voipLog?.log("onTaskRemoved --> ${rootIntent}")
        super.onTaskRemoved(rootIntent)
    }

    override fun onLowMemory() {
        super.onLowMemory()
        voipLog?.log("Service on Low Memory")
    }

    override fun onUnbind(intent: Intent?): Boolean {
        voipLog?.log("Service UnBinding")
        //showNotification()
        return true
    }

    override fun onRebind(intent: Intent?) {
        super.onRebind(intent)
        hideNotification()
        voipLog?.log("Service rebinding")
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        voipLog?.log("Service Trim Memory ")
    }

    private fun hideNotification() {
        stopForeground(true)
    }

    private fun showNotification() {

    }

}