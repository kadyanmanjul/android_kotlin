package com.joshtalks.joshskills.voip.data

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.app.Service
import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import android.os.IBinder
import android.os.Messenger
import android.os.Process
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
import com.joshtalks.joshskills.base.constants.INCOMING_CALL_ID
import com.joshtalks.joshskills.base.constants.INCOMING_CALL_URI
import com.joshtalks.joshskills.base.constants.PREF_KEY_MAIN_PROCESS_PID
import com.joshtalks.joshskills.base.constants.SERVICE_ACTION_MAIN_PROCESS_IN_BACKGROUND
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
import com.joshtalks.joshskills.voip.audiomanager.SOUND_TYPE_RINGTONE
import com.joshtalks.joshskills.voip.audiomanager.SoundManager
import com.joshtalks.joshskills.voip.calldetails.CallDetails
import com.joshtalks.joshskills.voip.calldetails.IncomingCallData
import com.joshtalks.joshskills.voip.communication.constants.ServerConstants
import com.joshtalks.joshskills.voip.communication.model.IncomingCall
import com.joshtalks.joshskills.voip.communication.model.NetworkAction
import com.joshtalks.joshskills.voip.communication.model.UserAction
import com.joshtalks.joshskills.voip.constant.CALL_CONNECTED_EVENT
import com.joshtalks.joshskills.voip.constant.CALL_CONNECT_REQUEST
import com.joshtalks.joshskills.voip.constant.CALL_DISCONNECT_REQUEST
import com.joshtalks.joshskills.voip.constant.HOLD
import com.joshtalks.joshskills.voip.constant.IDLE
import com.joshtalks.joshskills.voip.constant.INCOMING_CALL
import com.joshtalks.joshskills.voip.constant.MUTE
import com.joshtalks.joshskills.voip.constant.SPEAKER_OFF_REQUEST
import com.joshtalks.joshskills.voip.constant.SPEAKER_ON_REQUEST
import com.joshtalks.joshskills.voip.constant.SWITCHED_TO_SPEAKER
import com.joshtalks.joshskills.voip.constant.SWITCHED_TO_WIRED
import com.joshtalks.joshskills.voip.constant.UNHOLD
import com.joshtalks.joshskills.voip.constant.UNMUTE
import com.joshtalks.joshskills.voip.mediator.CallServiceMediator
import com.joshtalks.joshskills.voip.mediator.CallingMediator
import com.joshtalks.joshskills.voip.notification.NotificationData
import com.joshtalks.joshskills.voip.notification.NotificationPriority
import com.joshtalks.joshskills.voip.notification.VoipNotification
import com.joshtalks.joshskills.voip.pstn.PSTNController
import com.joshtalks.joshskills.voip.pstn.PSTNState
import com.joshtalks.joshskills.voip.voipLog
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import timber.log.Timber

private const val TAG = "CallingRemoteService"
const val SERVICE_ALONE_LIFE_TIME = 1 * 60 * 1000L

class CallingRemoteService : Service() {
    private val coroutineExceptionHandler = CoroutineExceptionHandler{_, e ->
        Timber.tag("Coroutine Exception").d("Handled...")
        e.printStackTrace()
    }
    private val ioScope by lazy { CoroutineScope(Dispatchers.IO + coroutineExceptionHandler) }
    private val mediator by lazy<CallServiceMediator> { CallingMediator(ioScope) }
    private val handler by lazy { CallingRemoteServiceHandler.getInstance(ioScope) }
    private var isMediatorInitialise = false
    private val pstnController by lazy {
        PSTNController(ioScope)
    }
    private val audioController : AudioControllerInterface by lazy { AudioController(ioScope) }

    // For Testing Purpose
    private val notificationData = TestNotification()
    private val notification by lazy {
      VoipNotification(notificationData,NotificationPriority.Low)
    }
    private lateinit var serviceKillJob : Job

    override fun onCreate() {
        super.onCreate()
        updateStartCallTime(0)
        updateVoipState(IDLE)
        registerReceivers()
        observerPstnService()
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
        val checkAppState = (intent?.action == SERVICE_ACTION_MAIN_PROCESS_IN_BACKGROUND)
        Log.d(TAG, "onStartCommand: In BackGround $checkAppState")
        if(checkAppState) {
            startAutoServiceKillingTimer()
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
                            callType = CallDetails.callType,
                            currentUserAgoraId = CallDetails.localUserAgoraId,
                            channelName = CallDetails.agoraChannelName,
                            topicName = CallDetails.topicName
                        )
                        CALL_DISCONNECT_REQUEST -> updateLastCallDetails()

                        INCOMING_CALL -> {
                            updateIncomingCallDetails()
                            val data = IncomingCall(callId = IncomingCallData.callId)
                            mediator.showIncomingCall(data)
                        }
                    }
                    voipLog?.log("Sending Event to client")
                        handler.sendMessageToRepository(it)
                }
            }

            ioScope.launch {
                mediator.observeState().collect {
                    updateVoipState(it)
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

    private fun startAutoServiceKillingTimer() {
        Log.d(TAG, "startAutoServiceKillingTimer: ")
        serviceKillJob = ioScope.launch {
            delay(SERVICE_ALONE_LIFE_TIME)
            if(isActive)
                stopSelf()
        }
    }

    private fun stopServiceKillingTimer() {
        Log.d(TAG, "stopServiceKillingTimer: ")
        if(::serviceKillJob.isInitialized)
            serviceKillJob.cancel()
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

    private fun observerPstnService() {
        voipLog?.log("Listining PSTN")
        ioScope.launch {
            pstnController.observePSTNState().collect {
                when (it) {
                    PSTNState.Idle -> {
                        voipLog?.log("IDEL")
                        val data =
                            UserAction(type = ServerConstants.RESUME, callId = CallDetails.callId)
                        mediator.sendEventToServer(data)
                        handler.sendMessageToRepository(UNHOLD)
                    }
                    PSTNState.OnCall, PSTNState.Ringing -> {
                        voipLog?.log("ON CALL")
                        val data = UserAction(
                            type = ServerConstants.ONHOLD,
                            callId = CallDetails.callId
                        )
                        mediator.sendEventToServer(data)
                        handler.sendMessageToRepository(HOLD)
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
        //showNotification()
        if(::serviceKillJob.isInitialized.not())
            startAutoServiceKillingTimer()
        else if(serviceKillJob.isActive.not())
            startAutoServiceKillingTimer()

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

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceivers()
    }

    private fun showNotification() {
        startForeground(notification.getNotificationId(), notification.getNotificationObject().build())
    }

    private fun registerReceivers() {
        pstnController.registerPstnReceiver()
        audioController.registerAudioControllerReceivers()
    }

    private fun unregisterReceivers() {
        pstnController.unregisterPstnReceiver()
        audioController.unregisterAudioControllerReceivers()
    }

    private fun callDuration() : Long {
        val startTime = getStartCallTime()
        val currentTime = SystemClock.elapsedRealtime()
        return TimeUnit.MILLISECONDS.toSeconds(currentTime - startTime)
    }

    private fun updateStartCallTime(
        timestamp: Long,
        remoteUserName: String = "",
        remoteUserImage: String? = null,
        callId: Int = -1,
        callType: Int = -1,
        remoteUserAgoraId: Int = -1,
        currentUserAgoraId:Int = -1,
        channelName : String = "",
        topicName : String = ""
    ) {
        voipLog?.log("QUERY")
        val values = ContentValues(9).apply {
            put(CALL_START_TIME, timestamp)
            put(REMOTE_USER_NAME, remoteUserName)
            put(REMOTE_USER_IMAGE, remoteUserImage)
            put(REMOTE_USER_AGORA_ID, remoteUserAgoraId)
            put(CALL_ID, callId)
            put(CALL_TYPE, callType)
            put(CHANNEL_NAME, channelName)
            put(TOPIC_NAME, topicName)
            put(CURRENT_USER_AGORA_ID, currentUserAgoraId)
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

    private fun updateIncomingCallDetails() {
        voipLog?.log("QUERY")
        val values = ContentValues(2).apply {
            put(CALL_ID, IncomingCallData.callId)
            put(CALL_TYPE, IncomingCallData.callType)
        }
        val data = contentResolver.insert(
            Uri.parse(CONTENT_URI + INCOMING_CALL_URI),
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