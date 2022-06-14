package com.joshtalks.joshskills.voip.data

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import com.joshtalks.joshskills.base.constants.*
import com.joshtalks.joshskills.voip.*
import com.joshtalks.joshskills.voip.audiocontroller.AudioController
import com.joshtalks.joshskills.voip.audiocontroller.AudioControllerInterface
import com.joshtalks.joshskills.voip.audiocontroller.AudioRouteConstants
import com.joshtalks.joshskills.voip.calldetails.IncomingCallData
import com.joshtalks.joshskills.voip.communication.model.*
import com.joshtalks.joshskills.voip.constant.*
import com.joshtalks.joshskills.voip.constant.Event.*
import com.joshtalks.joshskills.voip.constant.Event.CALL_CONNECTED_EVENT
import com.joshtalks.joshskills.voip.constant.Event.CALL_INITIATED_EVENT
import com.joshtalks.joshskills.voip.constant.Event.CLOSE_CALL_SCREEN
import com.joshtalks.joshskills.voip.constant.Event.RECONNECTING_FAILED
import com.joshtalks.joshskills.voip.data.local.PrefManager
import com.joshtalks.joshskills.voip.mediator.CallCategory
import com.joshtalks.joshskills.voip.mediator.CallServiceMediator
import com.joshtalks.joshskills.voip.mediator.CallingMediator
import com.joshtalks.joshskills.voip.notification.IncomingCallNotificationHandler
import com.joshtalks.joshskills.voip.notification.NotificationData
import com.joshtalks.joshskills.voip.notification.NotificationPriority
import com.joshtalks.joshskills.voip.notification.VoipNotification
import com.joshtalks.joshskills.voip.pstn.PSTNController
import com.joshtalks.joshskills.voip.pstn.PSTNState
import com.joshtalks.joshskills.voip.state.CallConnectData
import com.joshtalks.joshskills.voip.voipanalytics.CallAnalytics
import com.joshtalks.joshskills.voip.voipanalytics.EventName
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import com.joshtalks.joshskills.voip.mediator.UserAction as Action
import timber.log.Timber
import com.joshtalks.joshskills.base.model.NotificationData as Data

private const val TAG = "CallingRemoteService"
const val SERVICE_ALONE_LIFE_TIME = 1 * 60 * 1000L

class CallingRemoteService : Service() {
    private var isServiceInitialize = false
    private val coroutineExceptionHandler = CoroutineExceptionHandler { _, e ->
        Timber.tag("Coroutine Exception").d("Handled...")
        e.printStackTrace()
    }
    private val ioScope by lazy { CoroutineScope(Dispatchers.IO + coroutineExceptionHandler) }
    private val syncScope by lazy { CoroutineScope(Dispatchers.IO + coroutineExceptionHandler) }
    private val mediator by lazy<CallServiceMediator> { CallingMediator(ioScope) }
    private var isMediatorInitialise = false
    private val pstnController by lazy { PSTNController(ioScope) }
    private val audioController: AudioControllerInterface by lazy { AudioController(ioScope) }
    private val serviceEvents = MutableSharedFlow<ServiceEvents>(replay = 0)

    // For Testing Purpose
    private val notificationData by lazy { TestNotification(getNotificationData()) }
    private val notification by lazy { VoipNotification(notificationData, NotificationPriority.Low) }
    private val binder = RemoteServiceBinder()

    override fun onCreate() {
        Log.d(TAG, "onCreate: ")
        super.onCreate()
        PrefManager.initServicePref(this)
        PrefManager.setVoipState(State.IDLE)
        updateStartTime(0)
        syncScope.launch {
            Utils.syncAnalytics()
        }
        registerReceivers()
        observerPstnService()
        observeAudio()
        showNotification()
        Log.d(TAG, "onCreate: Creating Service")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "StartService --- OnStartCommand")
        when(intent?.action) {
            SERVICE_ACTION_INCOMING_CALL -> {
                val map = HashMap<String,String>()
                map[INCOMING_CALL_CATEGORY] = Category.PEER_TO_PEER.category
                map[com.joshtalks.joshskills.voip.constant.INCOMING_CALL_ID] = "222"
                ioScope.launch { mediator.handleIncomingCall(map) }
            }
            SERVICE_ACTION_STOP_SERVICE -> {
                // TODO: Might Need to refactor
                if (PrefManager.getVoipState() == State.CONNECTED) {
                    disconnectCall()
                }
                ioScope.cancel()
                syncScope.cancel()
                stopSelf()
                return START_NOT_STICKY
            }
            SERVICE_ACTION_DISCONNECT_CALL -> {
                CallAnalytics.addAnalytics(
                    event = EventName.DISCONNECTED_BY_HANG_UP,
                    agoraCallId = PrefManager.getAgraCallId().toString(),
                    agoraMentorId = PrefManager.getLocalUserAgoraId().toString()
                )
                disconnectCall()
                return START_NOT_STICKY
            }
            SERVICE_ACTION_INCOMING_CALL_DECLINE -> {
                CallAnalytics.addAnalytics(
                    event = EventName.INCOMING_CALL_DECLINE,
                    agoraCallId ="-1",
                    agoraMentorId = PrefManager.getLocalUserAgoraId().toString()
                )
                IncomingCallNotificationHandler().removeNotification()
                return START_NOT_STICKY
            }
        }
        return if (isServiceInitialize)
            START_STICKY
        else
            intent.initService()
    }

    fun getUserDetails(): StateFlow<UIState> = mediator.observerUIState()

    fun getEvents(): SharedFlow<ServiceEvents> = serviceEvents

    private fun Intent?.initService(): Int {
        isServiceInitialize = true
        observeNetworkEvents()
        return START_REDELIVER_INTENT
    }

    private fun observeNetworkEvents() {
        if (isMediatorInitialise.not()) {
            isMediatorInitialise = true
            ioScope.launch {
                try {
                    mediator.observeEvents().collect {
                        try{
                            Log.d(TAG, "observeMediatorEvents: $it")
                            when (it.type) {
                                CALL_CONNECTED_EVENT -> {
                                    val data = it.data as CallConnectData
                                    updateStartTime(data.startTime)
                                    notification.connected(
                                        data.userName,
                                        openCallScreen(),
                                        getHangUpIntent()
                                    )
                                    serviceEvents.emit(ServiceEvents.CALL_CONNECTED_EVENT)
                                }
                                CLOSE_CALL_SCREEN -> {
                                    serviceEvents.emit(ServiceEvents.CLOSE_CALL_SCREEN)
                                    notification.idle(getNotificationData())
                                }
                                RECONNECTING_FAILED -> {
                                    serviceEvents.emit(ServiceEvents.RECONNECTING_FAILED)
                                    notification.idle(getNotificationData())
                                }
                                CALL_INITIATED_EVENT -> {
                                    serviceEvents.emit(ServiceEvents.CALL_INITIATED_EVENT)
                                }
                            }
                        }
                        catch (e : Exception){
                            if(e is CancellationException)
                                throw e
                            e.printStackTrace()
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    if(e is CancellationException)
                        throw e
                }
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder {
        Log.d(TAG, "onBind: ")
        return binder
    }

    inner class RemoteServiceBinder : Binder() {
        fun getService() = this@CallingRemoteService
    }

    private fun observerPstnService() {
        ioScope.launch {
            try {
                pstnController.observePSTNState().collect {
                    try{
                        when (it) {
                            PSTNState.Idle -> {
                                PrefManager.savePstnState(PSTN_STATE_IDLE)
                                mediator.userAction(Action.UNHOLD)
                            }
                            PSTNState.OnCall, PSTNState.Ringing -> {
                                PrefManager.savePstnState(PSTN_STATE_ONCALL)
                                mediator.userAction(Action.HOLD)
                            }
                        }
                    }
                    catch (e : Exception){
                        if(e is CancellationException)
                            throw e
                        e.printStackTrace()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                if(e is CancellationException)
                    throw e
            }
        }
    }

    private fun observeAudio() {
        ioScope.launch {
            try {
                audioController.observeAudioRoute().collect {
                    try{
                        Log.d(TAG, "observeAudio: $it")
                        when (it) {
                            AudioRouteConstants.BluetoothAudio -> {}
                            AudioRouteConstants.Default -> {}
                            AudioRouteConstants.EarpieceAudio -> {}
                            AudioRouteConstants.HeadsetAudio -> {}
                            AudioRouteConstants.SpeakerAudio -> {}
                        }
                    }
                    catch (e : Exception){
                        if(e is CancellationException)
                            throw e
                        e.printStackTrace()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                if(e is CancellationException)
                    throw e
            }
        }
    }

    /**
     * Events Which Repository can Use --- Start
     */
    fun connectCall(callData: HashMap<String, Any>, category: Category = Category.PEER_TO_PEER) {
        if (callData != null) {
            mediator.connectCall(category, callData)
            Log.d(TAG, "Connecting Call Data --> $callData")
        } else
            Log.d(TAG, "connectCall: Call Data is Null")
    }

    fun disconnectCall() {
        notification.idle(getNotificationData())
        mediator.userAction(Action.DISCONNECT)
    }

    fun changeMicState(isMicOn: Boolean) {
        mediator.userAction(if (isMicOn) Action.UNMUTE else Action.MUTE)
    }

    fun changeSpeakerState(isSpeakerOn: Boolean) {
        if (isSpeakerOn)
            audioController.switchAudioToSpeaker()
        else
            audioController.switchAudioToDefault()
        mediator.userAction(if(isSpeakerOn) Action.SPEAKER_ON else Action.SPEAKER_OFF)
    }

    fun backPress() { mediator.userAction(Action.BACK_PRESS) }

    fun changeTopicImage() { mediator.userAction(Action.TOPIC_IMAGE_CHANGE) }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        stopForeground(true)
        stopSelf()
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy: ")
        unregisterReceivers()
        mediator.onDestroy()
        ioScope.cancel()
        syncScope.cancel()
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
}

// TODO: Need to Change
class TestNotification(val data : Data) : NotificationData {
    override fun setTitle(): String {
        return data.title
    }

    override fun setContent(): String {
        return data.subTitle
    }
}

// TODO: Should be in a new Class
data class UIState(
    val remoteUserName: String,
    val remoteUserImage: String?,
    val topicName: String,
    val callType: Int,
    val currentTopicImage: String,
    val occupation : String,
    val aspiration : String,
    val isOnHold: Boolean = false,
    val isSpeakerOn: Boolean = false,
    val isRemoteUserMuted: Boolean = false,
    val isOnMute: Boolean = false,
    val isReconnecting: Boolean = false,
    val startTime: Long = 0L
) {
    companion object {
        fun empty() = UIState("", null, "", 0,"","","")
    }
}

enum class ServiceEvents {
    CALL_INITIATED_EVENT,
    CALL_CONNECTED_EVENT,
    RECONNECTING_FAILED,
    CLOSE_CALL_SCREEN
}