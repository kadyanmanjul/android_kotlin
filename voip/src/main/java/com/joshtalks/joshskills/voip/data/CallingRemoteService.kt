package com.joshtalks.joshskills.voip.data

import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.os.Messenger
import android.os.SystemClock
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.joshtalks.joshskills.base.constants.INTENT_DATA_API_HEADER
import com.joshtalks.joshskills.base.constants.INTENT_DATA_MENTOR_ID
import com.joshtalks.joshskills.base.constants.PEER_TO_PEER
import com.joshtalks.joshskills.base.constants.SERVICE_ACTION_STOP_SERVICE
import com.joshtalks.joshskills.base.constants.SERVICE_ACTION_DISCONNECT_CALL
import com.joshtalks.joshskills.base.constants.SERVICE_ACTION_INCOMING_CALL_DECLINE
import com.joshtalks.joshskills.base.constants.SERVICE_ACTION_MAIN_PROCESS_IN_BACKGROUND
import com.joshtalks.joshskills.voip.Utils
import com.joshtalks.joshskills.voip.audiocontroller.AudioController
import com.joshtalks.joshskills.voip.audiocontroller.AudioControllerInterface
import com.joshtalks.joshskills.voip.audiocontroller.AudioRouteConstants.BluetoothAudio
import com.joshtalks.joshskills.voip.audiocontroller.AudioRouteConstants.EarpieceAudio
import com.joshtalks.joshskills.voip.audiocontroller.AudioRouteConstants.HeadsetAudio
import com.joshtalks.joshskills.voip.audiocontroller.AudioRouteConstants.SpeakerAudio
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
import com.joshtalks.joshskills.voip.data.local.PrefManager
import com.joshtalks.joshskills.voip.getHangUpIntent
import com.joshtalks.joshskills.voip.getStartCallTime
import com.joshtalks.joshskills.voip.mediator.CallServiceMediator
import com.joshtalks.joshskills.voip.mediator.CallingMediator
import com.joshtalks.joshskills.voip.notification.NotificationData
import com.joshtalks.joshskills.voip.notification.NotificationPriority
import com.joshtalks.joshskills.voip.notification.VoipNotification
import com.joshtalks.joshskills.voip.openCallScreen
import com.joshtalks.joshskills.voip.pstn.PSTNController
import com.joshtalks.joshskills.voip.pstn.PSTNState
import com.joshtalks.joshskills.voip.resetCallUIState
import com.joshtalks.joshskills.voip.updateIncomingCallDetails
import com.joshtalks.joshskills.voip.updateLastCallDetails
import com.joshtalks.joshskills.voip.updateRemoteUserMuteState
import com.joshtalks.joshskills.voip.updateStartCallTime
import com.joshtalks.joshskills.voip.updateUserHoldState
import com.joshtalks.joshskills.voip.updateVoipState
import com.joshtalks.joshskills.voip.voipLog
import kotlinx.coroutines.*
import java.time.Duration
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import timber.log.Timber

private const val TAG = "CallingRemoteService"
const val SERVICE_ALONE_LIFE_TIME = 1 * 60 * 1000L

class CallingRemoteService : Service() {
    private var isServiceInitialize = false
    private val coroutineExceptionHandler = CoroutineExceptionHandler { _, e ->
        Timber.tag("Coroutine Exception").d("Handled...")
        e.printStackTrace()
    }
    private val ioScope by lazy { CoroutineScope(Dispatchers.IO + coroutineExceptionHandler) }
    private val mediator by lazy<CallServiceMediator> { CallingMediator(ioScope) }
    private val handler by lazy { CallingRemoteServiceHandler.getInstance(ioScope) }
    private var currentState = IDLE
    private var isMediatorInitialise = false
    private val pstnController by lazy { PSTNController(ioScope) }
    private val audioController: AudioControllerInterface by lazy { AudioController(ioScope) }

    // For Testing Purpose
    private val notificationData = TestNotification()
    private val notification by lazy {
        VoipNotification(notificationData, NotificationPriority.Low)
    }
    private lateinit var serviceKillJob: Job

    override fun onCreate() {
        super.onCreate()
        PrefManager.initServicePref(this)
        stopServiceKillingTimer()
        updateStartCallTime(0)
        updateVoipState(IDLE)
        resetCallUIState()
        registerReceivers()
        observerPstnService()
        //observeAudioRouteEvents()
        voipLog?.log("Creating Service")
        showNotification()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        voipLog?.log("StartService --- OnStartCommand")
        val shouldStopService = (intent?.action == SERVICE_ACTION_STOP_SERVICE)
        if (currentState == IDLE && shouldStopService) {
            stopSelf()
            return START_NOT_STICKY
        }
        val checkAppState = (intent?.action == SERVICE_ACTION_MAIN_PROCESS_IN_BACKGROUND)
        Log.d(TAG, "onStartCommand: In BackGround $checkAppState")
        if (checkAppState) {
            startAutoServiceKillingTimer()
            return START_NOT_STICKY
        }
        val hungUpCall = (intent?.action == SERVICE_ACTION_DISCONNECT_CALL)
        Log.d(TAG, "onStartCommand: SERVICE_ACTION_DISCONNECT_CALL --> $hungUpCall")
        if (hungUpCall) {
            Log.d(TAG, "onStartCommand: hungUpCall")
            disconnectCall()
            return START_NOT_STICKY
        }
        val isCallDecline = (intent?.action == SERVICE_ACTION_INCOMING_CALL_DECLINE)
        Log.d(TAG, "onStartCommand: SERVICE_ACTION_INCOMING_CALL_DECLINE --> $hungUpCall")
        if (isCallDecline) {
            mediator.hideIncomingCall()
            return START_NOT_STICKY
        }
        return if(isServiceInitialize)
            START_STICKY
        else
            intent.initService()
    }

    private fun Intent?.initService() : Int {
        Utils.apiHeader = this?.getParcelableExtra(INTENT_DATA_API_HEADER)
        Utils.uuid = this?.getStringExtra(INTENT_DATA_MENTOR_ID)
        voipLog?.log("API Header --> ${Utils.apiHeader}")
        voipLog?.log("Mentor Id --> ${Utils.uuid}")
        // TODO: Refactor Code {Maybe use Content Provider}
        observeMediatorEvents()
        return START_REDELIVER_INTENT
    }

    private fun observeMediatorEvents() {
        if (isMediatorInitialise.not()) {
            isMediatorInitialise = true
            ioScope.launch {
                try {
                    mediator.observeEvents().collect {
                        when (it) {
                            CALL_CONNECTED_EVENT -> {
                                updateStartCallTime(
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
                                notification.connected(
                                    CallDetails.remoteUserName,
                                    openCallScreen(),
                                    getHangUpIntent()
                                )
                            }
                            CALL_DISCONNECT_REQUEST -> {
                                Log.d(TAG, "onStartCommand: CALL_DISCONNECT_REQUEST")
                                notification.idle()
                                resetCallUIState()
                                val duration = callDuration()
                                if(duration > 0)
                                    updateLastCallDetails(duration)
                            }

                            INCOMING_CALL -> {
                                updateIncomingCallDetails()
                                val data = IncomingCall(callId = IncomingCallData.callId)
                                mediator.showIncomingCall(data)
                            }
                            MUTE -> updateRemoteUserMuteState(true)
                            UNMUTE -> updateRemoteUserMuteState(false)
                            HOLD -> updateUserHoldState(true)
                            UNHOLD -> updateUserHoldState(false)
                        }
                        voipLog?.log("Sending Event to client $it")
                        handler.sendMessageToRepository(it)
                    }
                } catch (e : Exception) {
                    e.printStackTrace()
                }
            }

            ioScope.launch {
                try {
                    mediator.observeState().collect {
                        currentState = it
                        updateVoipState(it)
                        voipLog?.log("State --> $it")
                    }
                } catch (e : Exception) {
                    e.printStackTrace()
                }
            }
        }
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
            try {
                delay(SERVICE_ALONE_LIFE_TIME)
                if (isActive)
                    stopSelf()
            } catch (e : Exception) {
                if(e is CancellationException)
                    throw e
                e.printStackTrace()
            }
        }
    }

    private fun stopServiceKillingTimer() {
        Log.d(TAG, "stopServiceKillingTimer: ")
        if (::serviceKillJob.isInitialized)
            serviceKillJob.cancel()
    }

    private fun observeAudioRouteEvents() {
        ioScope.launch {
            try {
                audioController.observeAudioRoute().collectLatest {
                    when (it) {
                        BluetoothAudio, EarpieceAudio, HeadsetAudio -> {
                            // TODO: Need to check
                            //updateUserSpeakerState(false)
                            handler.sendMessageToRepository(SWITCHED_TO_WIRED)
                        }
                        SpeakerAudio -> {
                            //updateUserSpeakerState(true)
                            handler.sendMessageToRepository(SWITCHED_TO_SPEAKER)
                        }
                    }
                }
            } catch (e : Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun observerPstnService() {
        voipLog?.log("Listining PSTN")
        ioScope.launch {
            try {
                pstnController.observePSTNState().collect {
                    when (it) {
                        PSTNState.Idle -> {
                            voipLog?.log("IDEL")
                            val data =
                                UserAction(
                                    type = ServerConstants.RESUME,
                                    channelName = CallDetails.agoraChannelName,
                                    address = CallDetails.partnerMentorId ?: (Utils.uuid ?: "")
                                )
                            mediator.sendEventToServer(data)
                            updateUserHoldState(false)
                        }
                        PSTNState.OnCall, PSTNState.Ringing -> {
                            voipLog?.log("ON CALL")
                            val data = UserAction(
                                type = ServerConstants.ONHOLD,
                                channelName = CallDetails.agoraChannelName,
                                address = CallDetails.partnerMentorId ?: (Utils.uuid ?: "")
                            )
                            mediator.sendEventToServer(data)
                            updateUserHoldState(true)
                        }
                    }
                }
            } catch (e : Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun observeHandlerEvents(handler: CallingRemoteServiceHandler) {
        voipLog?.log("${handler}")
        ioScope.launch {
            try {
                handler.observerFlow().collect {
                    when (it.what) {
                        CALL_CONNECT_REQUEST -> {
                            val callData = it.obj as? HashMap<String, Any>
                            if (callData != null) {
                                mediator.connectCall(PEER_TO_PEER, callData)
                                voipLog?.log("Connecting Call Data --> $callData")
                            } else
                                voipLog?.log("Mediator is NULL")
                        }
                        CALL_DISCONNECT_REQUEST -> {
                            Log.d(TAG, "observeHandlerEvents: CALL_DISCONNECT_REQUEST")
                            voipLog?.log("Disconnect Call")
                            disconnectCall()
                        }
                        MUTE -> {
                            val userAction = UserAction(
                                ServerConstants.MUTE,
                                CallDetails.agoraChannelName,
                                address = CallDetails.partnerMentorId ?: (Utils.uuid ?: "")
                            )
                            mediator.muteAudioStream(true)
                            mediator.sendEventToServer(userAction)
                        }
                        UNMUTE -> {
                            val userAction = UserAction(
                                ServerConstants.UNMUTE,
                                CallDetails.agoraChannelName,
                                address = CallDetails.partnerMentorId ?: (Utils.uuid ?: "")
                            )
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
            } catch (e : Exception) {
                e.printStackTrace()
            }
        }
    }


    private fun disconnectCall() {
        Log.d(TAG, "disconnectCall: ")
        val duration = callDuration()
        val networkAction = NetworkAction(
            channelName = CallDetails.agoraChannelName,
            uid = CallDetails.localUserAgoraId,
            type = ServerConstants.DISCONNECTED,
            duration = duration,
            address = CallDetails.partnerMentorId ?: (Utils.uuid ?: "")
        )
        notification.idle()
        resetCallUIState()
        Log.d(TAG, "disconnectCall: disconnectCall")
        if(duration > 0)
            updateLastCallDetails(duration)
        mediator.sendEventToServer(networkAction)
        mediator.disconnectCall()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        //showNotification()
        if (currentState == IDLE) {
            if (::serviceKillJob.isInitialized.not())
                startAutoServiceKillingTimer()
            else if (serviceKillJob.isActive.not())
                startAutoServiceKillingTimer()
        }

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
        startForeground(
            notification.getNotificationId(),
            notification.getNotificationObject().build()
        )
    }

    private fun registerReceivers() {
        pstnController.registerPstnReceiver()
        audioController.registerAudioControllerReceivers()
    }

    private fun unregisterReceivers() {
        pstnController.unregisterPstnReceiver()
        audioController.unregisterAudioControllerReceivers()
    }

    fun callDuration(): Long {
        val startTime = getStartCallTime()
        if(startTime == 0L)
            return 0L
        val currentTime = SystemClock.elapsedRealtime()
        Log.d(TAG, "callDuration: ST -> $startTime  and CT -> $currentTime")
        return TimeUnit.MILLISECONDS.toSeconds(currentTime - startTime)
    }

}

class TestNotification : NotificationData {
    override fun setTitle(): String {
        return "Josh Skills"
    }

    override fun setContent(): String {
        return "Quickly Learn English"
    }

    override fun setTapAction(): PendingIntent? {
        return super.setTapAction()
    }
}