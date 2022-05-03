package com.joshtalks.joshskills.voip.data

import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.os.Message
import android.os.SystemClock
import android.util.Log
import com.joshtalks.joshskills.base.constants.*
import com.joshtalks.joshskills.voip.*
import com.joshtalks.joshskills.voip.audiocontroller.AudioController
import com.joshtalks.joshskills.voip.audiocontroller.AudioControllerInterface
import com.joshtalks.joshskills.voip.audiocontroller.AudioRouteConstants.BluetoothAudio
import com.joshtalks.joshskills.voip.audiocontroller.AudioRouteConstants.EarpieceAudio
import com.joshtalks.joshskills.voip.audiocontroller.AudioRouteConstants.HeadsetAudio
import com.joshtalks.joshskills.voip.audiocontroller.AudioRouteConstants.SpeakerAudio
import com.joshtalks.joshskills.voip.calldetails.CallDetails
import com.joshtalks.joshskills.voip.calldetails.IncomingCallData
import com.joshtalks.joshskills.voip.communication.PubnubState
import com.joshtalks.joshskills.voip.communication.constants.ServerConstants
import com.joshtalks.joshskills.voip.communication.model.*
import com.joshtalks.joshskills.voip.constant.*
import com.joshtalks.joshskills.voip.data.local.PrefManager
import com.joshtalks.joshskills.voip.mediator.CallServiceMediator
import com.joshtalks.joshskills.voip.mediator.CallingMediator
import com.joshtalks.joshskills.voip.notification.NotificationData
import com.joshtalks.joshskills.voip.notification.NotificationPriority
import com.joshtalks.joshskills.voip.notification.VoipNotification
import com.joshtalks.joshskills.voip.presence.PresenceStatus
import com.joshtalks.joshskills.voip.presence.UserPresence
import com.joshtalks.joshskills.voip.pstn.PSTNController
import com.joshtalks.joshskills.voip.pstn.PSTNState
import com.joshtalks.joshskills.voip.voipanalytics.CallAnalytics
import com.joshtalks.joshskills.voip.voipanalytics.CallEvents
import com.joshtalks.joshskills.voip.voipanalytics.EventName
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.concurrent.TimeUnit
import timber.log.Timber

private const val TAG = "CallingRemoteService"
const val SERVICE_ALONE_LIFE_TIME = 1 * 60 * 1000L

class CallingRemoteService : Service() {
    private var isServiceInitialize = false
    private val coroutineExceptionHandler = CoroutineExceptionHandler { _, e ->
        Timber.tag("Coroutine Exception").d("Handled...")
        e.printStackTrace()
    }
    private val userPresence by lazy {
        UserPresence
    }
    private val ioScope by lazy { CoroutineScope(Dispatchers.IO + coroutineExceptionHandler) }
    private val mediator by lazy<CallServiceMediator> { CallingMediator(ioScope) }
    //private val handler by lazy { CallingRemoteServiceHandler.getInstance(ioScope) }
    var currentState = IDLE
    private set
    private var isMediatorInitialise = false
    private val pstnController by lazy {
        Log.d(TAG, "Creating : pstnController")
        PSTNController(ioScope)
    }
    private val audioController: AudioControllerInterface by lazy { AudioController(ioScope) }
    private val uiStateFlow = MutableStateFlow(UIState.empty())
    private val serviceEvents = MutableSharedFlow<ServiceEvents>(replay = 0)

    // For Testing Purpose
    private val notificationData = TestNotification()
    private val notification by lazy {
        VoipNotification(notificationData, NotificationPriority.Low)
    }
    private val binder = RemoteServiceBinder()
    private var currentUiState = UIState.empty()

    fun getUserDetails() : StateFlow<UIState> = uiStateFlow

    fun getEvents() : SharedFlow<ServiceEvents> = serviceEvents

    override fun onCreate() {
        Log.d(TAG, "onCreate: ")
        super.onCreate()
        PrefManager.initServicePref(this)
        //stopServiceKillingTimer()
        updateStartCallTime(0)
        updateVoipState(IDLE)
        //resetCallUIState()
        registerReceivers()
        observerPstnService()
        //observeAudioRouteEvents()
        voipLog?.log("Creating Service")
        showNotification()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        voipLog?.log("StartService --- OnStartCommand")
        val shouldStopService = (intent?.action == SERVICE_ACTION_STOP_SERVICE)
        if (shouldStopService) {
            if(currentState == CONNECTED) {
                disconnectCall()
            }
            ioScope.cancel()
            stopSelf()
            return START_NOT_STICKY
        }

        val hungUpCall = (intent?.action == SERVICE_ACTION_DISCONNECT_CALL)
        Log.d(TAG, "onStartCommand: SERVICE_ACTION_DISCONNECT_CALL --> $hungUpCall")
        if (hungUpCall) {
            Log.d(TAG, "onStartCommand: hungUpCall")
                CallAnalytics.addAnalytics(
                    event = EventName.DISCONNECTED_BY_HANG_UP,
                    agoraCallId = CallDetails.callId.toString(),
                    agoraMentorId = CallDetails.localUserAgoraId.toString()
                )
            disconnectCall()
            return START_NOT_STICKY
        }
        val isCallDecline = (intent?.action == SERVICE_ACTION_INCOMING_CALL_DECLINE)
        Log.d(TAG, "onStartCommand: SERVICE_ACTION_INCOMING_CALL_DECLINE --> $hungUpCall")
        if (isCallDecline) {
            mediator.hideIncomingCall()
            CallAnalytics.addAnalytics(
                event = EventName.INCOMING_CALL_DECLINE
            )
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
//        setting status online on start service
        userPresence.setUserPresence(Utils.uuid.toString(),PresenceStatus.Online)
        observeNetworkEvents()
        return START_REDELIVER_INTENT
    }

    private fun observeNetworkEvents() {
        Log.d(TAG, "observeNetworkEvents: ${mediator.hashCode()}")
        if (isMediatorInitialise.not()) {
            isMediatorInitialise = true
            ioScope.launch {
                try {
                    mediator.observeEvents().collect {
                        Log.d(TAG, "observeMediatorEvents: $it")
                        when (it.what) {
                            CALL_CONNECTED_EVENT -> {
                                val startTime = SystemClock.elapsedRealtime()
                                updateStartCallTime(
                                    startTime,
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
                                serviceEvents.emit(ServiceEvents.CALL_CONNECTED_EVENT)
                                currentUiState = currentUiState.copy(startTime = startTime)
                                uiStateFlow.value = currentUiState
                            }
                            RECONNECTING -> {
                                CallAnalytics.addAnalytics(
                                    event = EventName.CALL_RECONNECTING,
                                    agoraCallId = CallDetails.callId.toString(),
                                    agoraMentorId = CallDetails.localUserAgoraId.toString()
                                )
                                currentUiState = currentUiState.copy(isReconnecting = true)
                                uiStateFlow.value = currentUiState
                            }
                            RECONNECTED -> {
                                currentUiState = currentUiState.copy(isReconnecting = false)
                                uiStateFlow.value = currentUiState
                                Log.d(TAG, "-- RECONNECTED -- : $currentUiState")
                                mediator.sendEventToServer(UI(
                                    channelName = CallDetails.agoraChannelName,
                                    type = ServerConstants.UI_STATE_UPDATED,
                                    isHold = if(currentUiState.isOnHold) 1 else 0,
                                    isMute = if(currentUiState.isOnMute) 1 else 0,
                                    address = CallDetails.partnerMentorId ?: ""
                                ))
                                Log.d(TAG, "-- RECONNECTED SENT -- : $currentUiState")
                            }
                            RECEIVED_CHANNEL_DATA -> {
                                val channelData = it.obj as? ChannelData
                                currentUiState = UIState(
                                    remoteUserImage = channelData?.getCallingPartnerImage(),
                                    remoteUserName = channelData?.getCallingPartnerName() ?: "",
                                    callType = channelData?.getType() ?: 0,
                                    topicName = channelData?.getCallingTopic() ?: ""
                                )
                                uiStateFlow.value = currentUiState
                            }
                            UI_STATE_UPDATED -> {
                                val uiData = it.obj as UI
                                if(uiData.getType() == ServerConstants.UI_STATE_UPDATED)
                                mediator.sendEventToServer(UI(
                                    channelName = CallDetails.agoraChannelName,
                                    type = ServerConstants.ACK_UI_STATE_UPDATED,
                                    isHold = if(currentUiState.isOnHold) 1 else 0,
                                    isMute = if(currentUiState.isOnMute) 1 else 0,
                                    address = CallDetails.partnerMentorId ?: ""
                                ))
                                Log.d(TAG, "-- UI_STATE -- : ${currentUiState}")
                                currentUiState = currentUiState.copy(
                                    isRemoteUserMuted = uiData.isMute(),
                                    isOnHold = uiData.isHold()
                                )
                                Log.d(TAG, "-- UI_STATE_UPDATED -- : ${currentUiState}")
                                uiStateFlow.value = currentUiState
                            }

                            CALL_DISCONNECT_REQUEST -> {
                                Log.d(TAG, "observeMediatorEvents: CALL_DISCONNECT_REQUEST")
                                serviceEvents.emit(ServiceEvents.CALL_DISCONNECT_REQUEST)
                                currentUiState = UIState.empty()
                                uiStateFlow.value = currentUiState
                                notification.idle()
                                //resetCallUIState()
                                updateVoipState(LEAVING)
                                PrefManager.setVoipState(LEAVING)
                                val duration = callDurationInMillis()
                                if(duration > 0)
                                    updateLastCallDetails(duration.inSeconds())
                            }

                            RECONNECTING_FAILED -> {
                                Log.d(TAG, "observeMediatorEvents: RECONNECTING_FAILED")
                                CallAnalytics.addAnalytics(
                                    event = EventName.DISCONNECTED_BY_RECONNECTING,
                                    agoraCallId = CallDetails.callId.toString(),
                                    agoraMentorId = CallDetails.localUserAgoraId.toString()
                                )
                                serviceEvents.emit(ServiceEvents.RECONNECTING_FAILED)
                                currentUiState = UIState.empty()
                                uiStateFlow.value = currentUiState
                                notification.idle()
                                //resetCallUIState()
                                updateVoipState(LEAVING)
                                PrefManager.setVoipState(LEAVING)
                                val duration = callDurationInMillis()
                                if(duration > 0)
                                    updateLastCallDetails(duration.inSeconds())
                                val networkAction = NetworkAction(
                                    channelName = CallDetails.agoraChannelName,
                                    uid = CallDetails.localUserAgoraId,
                                    type = ServerConstants.DISCONNECTED,
                                    duration = duration.inSeconds(),
                                    address = CallDetails.partnerMentorId ?: (Utils.uuid ?: "")
                                )
                                mediator.sendEventToServer(networkAction)
                                CallDetails.reset()
                            }

                            INCOMING_CALL -> {
                                CallAnalytics.addAnalytics(
                                    event = EventName.INCOMING_CALL_RECEIVED,
                                    agoraCallId = IncomingCallData.callId.toString()
                                )
                                currentUiState = UIState.empty().copy(callType = IncomingCallData.callType)
                                uiStateFlow.value = currentUiState
                                PrefManager.setIncomingCallId(IncomingCallData.callId)
                                //updateIncomingCallDetails()
                                val data = IncomingCall(callId = IncomingCallData.callId)
                                mediator.showIncomingCall(data)
                            }
                            MUTE -> {
                                currentUiState = currentUiState.copy(isRemoteUserMuted = true)
                                uiStateFlow.value = currentUiState
                                //updateRemoteUserMuteState(true)
                            }
                            UNMUTE -> {
                                currentUiState = currentUiState.copy(isRemoteUserMuted = false)
                                uiStateFlow.value = currentUiState
                                //updateRemoteUserMuteState(false)
                            }
                            HOLD -> {
                                currentUiState = currentUiState.copy(isOnHold = true)
                                uiStateFlow.value = currentUiState
                                //updateUserHoldState(true)
                            }
                            UNHOLD -> {
                                currentUiState = currentUiState.copy(isOnHold = false)
                                uiStateFlow.value = currentUiState
                                //updateUserHoldState(false)
                            }
                            CALL_INITIATED_EVENT -> {
                                serviceEvents.emit(ServiceEvents.CALL_INITIATED_EVENT)
                            }
                        }
                    }
                } catch (e : Exception) {
                    e.printStackTrace()
                }
            }

            ioScope.launch {
                try {
                    mediator.observeChannelState().collect {
                        when(it) {
                            PubnubState.CONNECTED -> {

                            }
                            PubnubState.RECONNECTED -> {
                                if(currentState == CONNECTED)
                                    sendCurrentUIState()
                            }
                            PubnubState.DISCONNECTED -> {

                            }
                        }
                    }
                } catch (e: Exception) {

                }
            }

            ioScope.launch {
                try {
                    mediator.observeState().collect {
                        currentState = it
                        updateVoipState(it)
                        PrefManager.setVoipState(it)
                        Log.d(TAG, "observeNetworkEvents: ---> $it")
                        voipLog?.log("State --> $it")
                    }
                } catch (e : Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun sendCurrentUIState() {
        mediator.sendEventToServer(UI(
            channelName = CallDetails.agoraChannelName,
            type = ServerConstants.UI_STATE_UPDATED,
            isHold = if(currentUiState.isOnHold) 1 else 0,
            isMute = if(currentUiState.isOnMute) 1 else 0,
            address = CallDetails.partnerMentorId ?: ""
        ))
    }

    override fun onBind(intent: Intent?): IBinder {
        Log.d(TAG, "onBind: ")
        return binder
    }

    inner class RemoteServiceBinder : Binder() {
        fun getService() = this@CallingRemoteService
    }

//    override fun onUnbind(intent: Intent?): Boolean {
//        voipLog?.log("Service Unbinding")
//        return true
//    }

    private fun observerPstnService() {
        voipLog?.log("Listining PSTN")
        ioScope.launch {
            try {
                pstnController.observePSTNState().collect {
                    when (it) {
                        PSTNState.Idle -> {
                            voipLog?.log("IDLE")
                            val data =
                                UserAction(
                                    type = ServerConstants.RESUME,
                                    channelName = CallDetails.agoraChannelName,
                                    address = CallDetails.partnerMentorId ?: (Utils.uuid ?: "")
                                )
                            mediator.sendEventToServer(data)
                            currentUiState = currentUiState.copy(isOnHold = false)
                            uiStateFlow.value = currentUiState
                            //updateUserHoldState(false)
                        }
                        PSTNState.OnCall, PSTNState.Ringing -> {
                            voipLog?.log("ON CALL")
                            val data = UserAction(
                                type = ServerConstants.ONHOLD,
                                channelName = CallDetails.agoraChannelName,
                                address = CallDetails.partnerMentorId ?: (Utils.uuid ?: "")
                            )
                            CallAnalytics.addAnalytics(
                                event = EventName.PSTN_CALL_RECEIVED,
                                agoraMentorId =  CallDetails.localUserAgoraId.toString(),
                                agoraCallId = CallDetails.callId.toString()
                            )

                            mediator.sendEventToServer(data)
                            currentUiState = currentUiState.copy(isOnHold = false)
                            uiStateFlow.value = currentUiState
                            //updateUserHoldState(true)
                        }
                    }
                }
            } catch (e : Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Events Which Repository can Use --- Start
     */
    fun connectCall(callData : HashMap<String, Any>) {
        if (callData != null) {
            mediator.connectCall(PEER_TO_PEER, callData)
            voipLog?.log("Connecting Call Data --> $callData")
        } else
            voipLog?.log("Mediator is NULL")
    }

    fun disconnectCall() {
        Log.d(TAG, "disconnectCall: ")
        val duration = callDurationInMillis()
        val networkAction = NetworkAction(
            channelName = CallDetails.agoraChannelName,
            uid = CallDetails.localUserAgoraId,
            type = ServerConstants.DISCONNECTED,
            duration = duration.inSeconds(),
            address = CallDetails.partnerMentorId ?: (Utils.uuid ?: "")
        )
        notification.idle()
        currentUiState = UIState.empty()
        uiStateFlow.value = currentUiState
        //resetCallUIState()
        Log.d(TAG, "disconnectCall: disconnectCall")
        if(duration > 0)
            updateLastCallDetails(duration.inSeconds())
        mediator.sendEventToServer(networkAction)
        mediator.disconnectCall()
    }

    fun changeMicState(isMicOn : Boolean) {
        currentUiState = currentUiState.copy(isOnMute = isMicOn.not())
        uiStateFlow.value = currentUiState
        val userAction = UserAction(
            if(isMicOn) ServerConstants.UNMUTE else ServerConstants.MUTE,
            CallDetails.agoraChannelName,
            address = CallDetails.partnerMentorId ?: (Utils.uuid ?: "")
        )
        mediator.muteAudioStream(isMicOn.not())
        mediator.sendEventToServer(userAction)
    }

    fun changeSpeakerState(isSpeakerOn : Boolean) {
        currentUiState = currentUiState.copy(isSpeakerOn = isSpeakerOn)
        uiStateFlow.value = currentUiState
        mediator.enableSpeaker(isSpeakerOn)
        if(isSpeakerOn)
            audioController.switchAudioToSpeaker()
        else
            audioController.switchAudioToDefault()
    }

    fun backPress() {}

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        stopForeground(true)
        stopSelf()
    }

//    override fun onTaskRemoved(rootIntent: Intent?) {
//        //showNotification()
//        Log.d(TAG, "onTaskRemoved: $currentState")
//        if(currentState == CONNECTED) {
//            disconnectCall()
//        }
//        ioScope.cancel()
//        stopSelf()
//        voipLog?.log("onTaskRemoved --> ${rootIntent}")
//        super.onTaskRemoved(rootIntent)
//    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy: ")
        unregisterReceivers()
        mediator.onDestroy()
        ioScope.cancel()
        super.onDestroy()
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

    fun callDurationInMillis(): Long {
        val startTime = getStartCallTime()
        if(startTime == 0L)
            return 0L
        val currentTime = SystemClock.elapsedRealtime()
        Log.d(TAG, "callDuration: ST -> $startTime  and CT -> $currentTime")
        return currentTime - startTime
    }

    fun Long.inSeconds() : Long {
        return TimeUnit.MILLISECONDS.toSeconds(this)
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

data class UIState(
    val remoteUserName : String,
    val remoteUserImage : String?,
    val topicName : String,
    val callType : Int,
    val isOnHold : Boolean = false,
    val isSpeakerOn: Boolean = false,
    val isRemoteUserMuted: Boolean = false,
    val isOnMute : Boolean = false,
    val isReconnecting : Boolean = false,
    val startTime : Long = 0L
) {
    companion object {
        fun empty() = UIState("", null,"", 0)
    }
}

enum class ServiceEvents {
    CALL_INITIATED_EVENT,
    CALL_CONNECTED_EVENT,
    CALL_DISCONNECT_REQUEST,
    RECONNECTING_FAILED
}