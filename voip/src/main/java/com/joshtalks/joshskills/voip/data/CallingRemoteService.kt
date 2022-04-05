package com.joshtalks.joshskills.voip.data

import android.app.Service
import android.content.ContentValues
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.IBinder
import android.os.Messenger
import android.os.SystemClock
import android.util.Log
import com.joshtalks.joshskills.base.constants.CALL_ID
import com.joshtalks.joshskills.base.constants.CALL_START_TIME
import com.joshtalks.joshskills.base.constants.CALL_TYPE
import com.joshtalks.joshskills.base.constants.CONTENT_URI
import com.joshtalks.joshskills.base.constants.INTENT_DATA_API_HEADER
import com.joshtalks.joshskills.base.constants.INTENT_DATA_MENTOR_ID
import com.joshtalks.joshskills.base.constants.PEER_TO_PEER
import com.joshtalks.joshskills.base.constants.REMOTE_USER_AGORA_ID
import com.joshtalks.joshskills.base.constants.REMOTE_USER_IMAGE
import com.joshtalks.joshskills.base.constants.REMOTE_USER_NAME
import com.joshtalks.joshskills.base.constants.SERVICE_ACTION_STOP_SERVICE
import com.joshtalks.joshskills.base.constants.CALL_DISCONNECTED_URI
import com.joshtalks.joshskills.base.constants.START_CALL_TIME_COLUMN
import com.joshtalks.joshskills.base.constants.START_CALL_TIME_URI
import com.joshtalks.joshskills.base.constants.VOIP_STATE_URI
import com.joshtalks.joshskills.base.constants.VOIP_STATE
import com.joshtalks.joshskills.voip.Utils
import com.joshtalks.joshskills.voip.audiocontroller.AudioController
import com.joshtalks.joshskills.voip.audiocontroller.AudioControllerInterface
import com.joshtalks.joshskills.voip.audiocontroller.AudioRouteConstants.BluetoothAudio
import com.joshtalks.joshskills.voip.audiocontroller.AudioRouteConstants.EarpieceAudio
import com.joshtalks.joshskills.voip.audiocontroller.AudioRouteConstants.HeadsetAudio
import com.joshtalks.joshskills.voip.audiocontroller.AudioRouteConstants.SpeakerAudio
import com.joshtalks.joshskills.voip.calldetails.CallDetails
import com.joshtalks.joshskills.voip.communication.constants.ServerConstants
import com.joshtalks.joshskills.voip.communication.model.NetworkAction
import com.joshtalks.joshskills.voip.communication.model.UserAction
import com.joshtalks.joshskills.voip.constant.CALL_CONNECTED_EVENT
import com.joshtalks.joshskills.voip.constant.CALL_CONNECT_REQUEST
import com.joshtalks.joshskills.voip.constant.CALL_DISCONNECT_REQUEST
import com.joshtalks.joshskills.voip.constant.IDLE
import com.joshtalks.joshskills.voip.constant.JOINING
import com.joshtalks.joshskills.voip.constant.LEAVING
import com.joshtalks.joshskills.voip.constant.MUTE
import com.joshtalks.joshskills.voip.constant.SPEAKER_OFF_REQUEST
import com.joshtalks.joshskills.voip.constant.SPEAKER_ON_REQUEST
import com.joshtalks.joshskills.voip.constant.SWITCHED_TO_SPEAKER
import com.joshtalks.joshskills.voip.constant.SWITCHED_TO_WIRED
import com.joshtalks.joshskills.voip.constant.UNMUTE
import com.joshtalks.joshskills.voip.mediator.CallServiceMediator
import com.joshtalks.joshskills.voip.mediator.CallingMediator
import com.joshtalks.joshskills.voip.notification.NotificationData
import com.joshtalks.joshskills.voip.notification.NotificationPriority
import com.joshtalks.joshskills.voip.notification.VoipNotification
import com.joshtalks.joshskills.voip.pstn.PSTNStateReceiver
import com.joshtalks.joshskills.voip.voipLog
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

private const val TAG = "CallingRemoteService"
class CallingRemoteService : Service() {
    private val ioScope by lazy { CoroutineScope(Dispatchers.IO) }
    private val mediator by lazy<CallServiceMediator> { CallingMediator(ioScope) }
    private val handler by lazy { CallingRemoteServiceHandler.getInstance(ioScope) }
    private var isMediatorInitialise = false
    private var pstnReceiver = PSTNStateReceiver()
    private val audioController : AudioControllerInterface by lazy { AudioController() }

    // For Testing Purpose
    private val notificationData = TestNotification()
    private val notification by lazy {
      VoipNotification(notificationData,NotificationPriority.Low)
    }

    override fun onCreate() {
        super.onCreate()
        updateStartCallTime(0)
        updateVoipState(IDLE)
        registerPstnCall()
        //registerAudioController()
        //observeAudioRouteEvents()
        voipLog?.log("Creating Service")
        showNotification()
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
                        CALL_CONNECTED_EVENT -> updateStartCallTime(
                            SystemClock.elapsedRealtime(),
                            remoteUserName = CallDetails.remoteUserName,
                            remoteUserImage = CallDetails.remoteUserImageUrl,
                            remoteUserAgoraId = CallDetails.remoteUserAgoraId,
                            callId = CallDetails.callId,
                            callType = CallDetails.callType
                        )
                        CALL_DISCONNECT_REQUEST -> updateLastCallDetails()
                    }
                    voipLog?.log("Sending Event to client")
                        handler.sendMessageToRepository(it)
                }
            }

            ioScope.launch {
                mediator.observeState().collect {
                    when(it) {
                        IDLE, JOINING, LEAVING -> {
                            updateVoipState(it)
                        }
                    }
                    voipLog?.log("State --> $it")
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

    private fun observeAudioRouteEvents() {
        ioScope.launch {
            audioController.observeAudioRoute().collectLatest {
                when(it) {
                    BluetoothAudio, EarpieceAudio, HeadsetAudio -> {
                        handler.sendMessageToRepository(SWITCHED_TO_WIRED)
                    }
                    SpeakerAudio -> {
                        handler.sendMessageToRepository(SWITCHED_TO_SPEAKER)
                    }
                }
            }
        }
    }

    private fun observeHandlerEvents(handler: CallingRemoteServiceHandler) {
        voipLog?.log("${handler}")
        ioScope.launch {
            handler.observerFlow().collect {
                when(it.what) {
                    CALL_CONNECT_REQUEST -> {
                        val callData = it.obj as? HashMap<String, Any>
                        if(callData != null) {
                            mediator.connectCall(PEER_TO_PEER, callData)
                            voipLog?.log("Connecting Call Data --> $callData")
                        }
                        else
                            voipLog?.log("Mediator is NULL")
                    }
                    CALL_DISCONNECT_REQUEST -> {
                        voipLog?.log("Disconnect Call")
                        val networkAction = NetworkAction(
                            callId = CallDetails.callId,
                            uid = CallDetails.localUserAgoraId,
                            type = ServerConstants.DISCONNECTED,
                            duration = callDuration()
                        )
                        updateLastCallDetails()
                        mediator.sendEventToServer(networkAction)
                        mediator.disconnectCall()
                    }
                    MUTE -> {
                        val userAction = UserAction(ServerConstants.MUTE, CallDetails.callId)
                        mediator.muteAudioStream(true)
                        mediator.sendEventToServer(userAction)
                    }
                    UNMUTE -> {
                        val userAction = UserAction(ServerConstants.UNMUTE, CallDetails.callId)
                        mediator.muteAudioStream(false)
                        mediator.sendEventToServer(userAction)
                    }
                    SPEAKER_ON_REQUEST -> {
                        audioController.switchAudioToSpeaker()
                    }
                    SPEAKER_OFF_REQUEST -> {
                        audioController.switchAudioToDefault()
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
        startForeground(notification.getNotificationId(), notification.getNotificationObject().build())
    }

    private fun registerPstnCall() {
        val filter = IntentFilter().apply {
            addAction("android.intent.action.PHONE_STATE")
            addAction("android.intent.action.NEW_OUTGOING_CALL")
        }
        registerReceiver(pstnReceiver, filter)
    }

    private fun registerAudioController() {
        audioController.registerAudioControllerReceivers()
    }

    private fun callDuration() : Long {
        val startTime = getStartCallTime()
        val currentTime = SystemClock.elapsedRealtime()
        Log.d(TAG, "callDuration: ST -> $startTime  and CT -> $currentTime")
        return TimeUnit.MILLISECONDS.toSeconds(currentTime - startTime)
    }

    private fun updateStartCallTime(
        timestamp: Long,
        remoteUserName: String = "",
        remoteUserImage: String? = null,
        callId: Int = -1,
        callType: Int = -1,
        remoteUserAgoraId: Int = -1
    ) {
        voipLog?.log("Timestamp -> $timestamp")
        val values = ContentValues(6).apply {
            put(CALL_START_TIME, timestamp)
            put(REMOTE_USER_NAME, remoteUserName)
            put(REMOTE_USER_IMAGE, remoteUserImage)
            put(REMOTE_USER_AGORA_ID, remoteUserAgoraId)
            put(CALL_ID, callId)
            put(CALL_TYPE, callType)
        }
        val data = contentResolver.insert(
            Uri.parse(CONTENT_URI + START_CALL_TIME_URI),
            values
        )
        voipLog?.log("Data --> $data")
    }

    private fun getStartCallTime() : Long {
        val startCallTimeCursor = contentResolver.query(
            Uri.parse(CONTENT_URI + START_CALL_TIME_URI),
            null,
            null,
            null,
            null
        )
        Log.d(TAG, "query: ${startCallTimeCursor?.columnNames?.asList()}")
        startCallTimeCursor?.moveToFirst()
        val startTime = startCallTimeCursor?.getLong(startCallTimeCursor.getColumnIndex(
            START_CALL_TIME_COLUMN))
        startCallTimeCursor?.close()
        Log.d(TAG, "getStartCallTime: $startTime")
        return startTime ?: 0L
    }

    private fun updateLastCallDetails() {
        voipLog?.log("QUERY")
        val values = ContentValues(1).apply {
            put(CALL_START_TIME, 0L)
        }
        val data = contentResolver.insert(
            Uri.parse(CONTENT_URI + CALL_DISCONNECTED_URI),
            values
        )
        voipLog?.log("Data --> $data")
    }

    private fun updateVoipState(state : Int) {
        voipLog?.log("Setting Voip State --> $state")
        val values = ContentValues(1).apply {
            put(VOIP_STATE, state)
        }
        val data = contentResolver.insert(
            Uri.parse(CONTENT_URI + VOIP_STATE_URI),
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