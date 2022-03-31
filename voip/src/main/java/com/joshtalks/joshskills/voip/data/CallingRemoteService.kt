package com.joshtalks.joshskills.voip.data

import android.app.Service
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Uri
import android.os.DeadObjectException
import android.os.IBinder
import android.os.Messenger
import android.os.SystemClock
import android.provider.UserDictionary
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.joshtalks.joshskills.base.constants.API_HEADER
import com.joshtalks.joshskills.base.constants.CALL_START_TIME
import com.joshtalks.joshskills.base.constants.CONTENT_URI
import com.joshtalks.joshskills.base.constants.INTENT_DATA_API_HEADER
import com.joshtalks.joshskills.base.constants.INTENT_DATA_MENTOR_ID
import com.joshtalks.joshskills.base.constants.SERVICE_ACTION_STOP_SERVICE
import com.joshtalks.joshskills.base.constants.UPDATE_START_CALL_TIME
import com.joshtalks.joshskills.voip.Utils
import com.joshtalks.joshskills.voip.constant.CALL_CONNECTED_EVENT
import com.joshtalks.joshskills.voip.constant.CALL_CONNECT_REQUEST
import com.joshtalks.joshskills.voip.constant.CALL_DISCONNECT_REQUEST
import com.joshtalks.joshskills.voip.mediator.CallServiceMediator
import com.joshtalks.joshskills.voip.mediator.CallType
import com.joshtalks.joshskills.voip.mediator.CallingMediator
import com.joshtalks.joshskills.voip.notification.NotificationData
import com.joshtalks.joshskills.voip.notification.NotificationHandler
import com.joshtalks.joshskills.voip.pstn.PSTNStateReceiver
import com.joshtalks.joshskills.voip.voipLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class CallingRemoteService : Service() {
    private val ioScope by lazy { CoroutineScope(Dispatchers.IO) }
    private val mediator by lazy<CallServiceMediator> { CallingMediator(ioScope) }
    private val handler by lazy { CallingRemoteServiceHandler.getInstance(ioScope) }
    private var isMediatorInitialise = false
    private var pstnReceiver = PSTNStateReceiver()

    // For Testing Purpose
    private val notificationData = TestNotification()
    private val notification by lazy {
        NotificationHandler(this)
        .getNotificationObject(notificationData)
    }

    override fun onCreate() {
        super.onCreate()
        registerPstnCall()
        voipLog?.log("Creating Service")
        showNotification()
        //hideNotification()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        voipLog?.log("StartService --- OnStartCommand")
        val shouldStopService = (intent?.action == SERVICE_ACTION_STOP_SERVICE)
        if(shouldStopService) {
            stopSelf()
            return START_NOT_STICKY
        }
        Utils.apiHeader = intent?.getParcelableExtra(INTENT_DATA_API_HEADER)
        Utils.uuid = intent?.getStringExtra(INTENT_DATA_MENTOR_ID)
        voipLog?.log("API Header --> ${Utils.apiHeader}")
        voipLog?.log("Mentor Id --> ${Utils.uuid}")
        // TODO: Refactor Code {Maybe use Content Provider}
        if(isMediatorInitialise.not()) {
            isMediatorInitialise = true
            ioScope.launch {
                mediator.observeEvents().collect {
                    when(it) {
                        CALL_CONNECTED_EVENT -> updateStartCallTime(SystemClock.elapsedRealtime())
                        CALL_DISCONNECT_REQUEST -> updateStartCallTime(0)
                    }
                    voipLog?.log("Sending Event to client")
                        handler.sendMessageToRepository(it)
                }
            }
        }
        return START_REDELIVER_INTENT
    }

    override fun onBind(intent: Intent?): IBinder? {
        voipLog?.log("Binding ....")
        val messenger = Messenger(handler)
        observeHandlerEvents(handler)
        return messenger.binder
    }

    override fun onUnbind(intent: Intent?): Boolean {
        voipLog?.log("Service Unbinding")
        return true
    }

    private fun observeHandlerEvents(handler: CallingRemoteServiceHandler) {
        voipLog?.log("${handler}")
        ioScope.launch {
            handler.observerFlow().collect {
                when(it.what) {
                    CALL_CONNECT_REQUEST -> {
                        val callData = it.obj as? HashMap<String, Any>
                        if(callData != null) {
                            mediator.connectCall(CallType.PEER_TO_PEER, callData)
                            voipLog?.log("Connecting Call Data --> $callData")
                        }
                        else
                            voipLog?.log("Mediator is NULL")
                    }
                    CALL_DISCONNECT_REQUEST -> {
                        voipLog?.log("Disconnect Call")
                        mediator.disconnectCall()
                        updateStartCallTime(0)
                    }
                }
                voipLog?.log("observeHandlerEvents: $it")
            }
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

    override fun onRebind(intent: Intent?) {
        super.onRebind(intent)
        voipLog?.log("Service rebinding")
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        voipLog?.log("Service Trim Memory ")
    }

    private fun showNotification() {
        startForeground(notification.notificationId, notification.notificationBuilder.build())
    }

    private fun registerPstnCall() {
        val filter = IntentFilter().apply {
            addAction("android.intent.action.PHONE_STATE")
            addAction("android.intent.action.NEW_OUTGOING_CALL")
        }
        registerReceiver(pstnReceiver, filter)
    }

    private fun updateStartCallTime(timeStamp : Long) {
        voipLog?.log("QUERY")
        val values = ContentValues(1).apply {
            put(CALL_START_TIME, timeStamp)
        }
        val data = contentResolver.insert(
            Uri.parse(CONTENT_URI + UPDATE_START_CALL_TIME),
            values
        )
        voipLog?.log("Data --> $data")
    }

}

class TestNotification : NotificationData {
    override fun setTitle(): String {
        return "Josh Skills"
    }

    override fun setContent(): String {
        return "Enjoy P2P Call"
    }

}