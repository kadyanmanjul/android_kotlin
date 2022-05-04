package com.joshtalks.joshskills.voip.data

import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.os.SystemClock
import android.util.Log
import com.joshtalks.joshskills.base.constants.INTENT_DATA_API_HEADER
import com.joshtalks.joshskills.base.constants.INTENT_DATA_MENTOR_ID
import com.joshtalks.joshskills.base.constants.PEER_TO_PEER
import com.joshtalks.joshskills.base.constants.SERVICE_ACTION_STOP_SERVICE
import com.joshtalks.joshskills.base.constants.SERVICE_ACTION_DISCONNECT_CALL
import com.joshtalks.joshskills.base.constants.SERVICE_ACTION_INCOMING_CALL_DECLINE
import com.joshtalks.joshskills.voip.*
import com.joshtalks.joshskills.voip.audiocontroller.AudioController
import com.joshtalks.joshskills.voip.audiocontroller.AudioControllerInterface
import com.joshtalks.joshskills.voip.calldetails.IncomingCallData
import com.joshtalks.joshskills.voip.calldetails.LastCallDetail
import com.joshtalks.joshskills.voip.communication.PubnubState
import com.joshtalks.joshskills.voip.communication.constants.ServerConstants
import com.joshtalks.joshskills.voip.communication.model.*
import com.joshtalks.joshskills.voip.constant.Event.*
import com.joshtalks.joshskills.voip.constant.IDLE
import com.joshtalks.joshskills.voip.constant.State
import com.joshtalks.joshskills.voip.data.local.PrefManager
import com.joshtalks.joshskills.voip.mediator.CallServiceMediator
import com.joshtalks.joshskills.voip.mediator.CallingMediator
import com.joshtalks.joshskills.voip.notification.NotificationData
import com.joshtalks.joshskills.voip.notification.NotificationPriority
import com.joshtalks.joshskills.voip.notification.VoipNotification
import com.joshtalks.joshskills.voip.pstn.PSTNController
import com.joshtalks.joshskills.voip.pstn.PSTNState
import com.joshtalks.joshskills.voip.state.CallConnectData
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import com.joshtalks.joshskills.voip.mediator.UserAction as Action
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
    private val ioScope by lazy { CoroutineScope(Dispatchers.IO + coroutineExceptionHandler) }
    private val mediator by lazy<CallServiceMediator> { CallingMediator(ioScope) }
    var currentState = IDLE
        private set
    private var isMediatorInitialise = false
    private val pstnController by lazy { PSTNController(ioScope) }
    private val audioController: AudioControllerInterface by lazy { AudioController(ioScope) }
    private val serviceEvents = MutableSharedFlow<ServiceEvents>(replay = 0)

    // For Testing Purpose
    private val notificationData = TestNotification()
    private val notification by lazy {
        VoipNotification(notificationData, NotificationPriority.Low)
    }
    private val binder = RemoteServiceBinder()

    override fun onCreate() {
        Log.d(TAG, "onCreate: ")
        super.onCreate()
        PrefManager.initServicePref(this)
        PrefManager.setVoipState(State.IDLE)
        updateStartTime(0)
        registerReceivers()
        observerPstnService()
        voipLog?.log("Creating Service")
        showNotification()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        voipLog?.log("StartService --- OnStartCommand")
        when(intent?.action) {
            SERVICE_ACTION_STOP_SERVICE -> {
                // TODO: Might Need to refactor
                if (PrefManager.getVoipState() == State.CONNECTED) {
                    disconnectCall()
                }
                ioScope.cancel()
                stopSelf()
                return START_NOT_STICKY
            }
            SERVICE_ACTION_DISCONNECT_CALL -> {
                disconnectCall()
                return START_NOT_STICKY
            }
            SERVICE_ACTION_INCOMING_CALL_DECLINE -> {
                mediator.hideIncomingCall()
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
        Utils.apiHeader = this?.getParcelableExtra(INTENT_DATA_API_HEADER)
        Utils.uuid = this?.getStringExtra(INTENT_DATA_MENTOR_ID)
        voipLog?.log("API Header --> ${Utils.apiHeader}")
        voipLog?.log("Mentor Id --> ${Utils.uuid}")
        // TODO: Refactor Code {Maybe use Content Provider}
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
                                    notification.idle()
                                }
                                RECONNECTING_FAILED -> {
                                    serviceEvents.emit(ServiceEvents.RECONNECTING_FAILED)
                                    notification.idle()
                                }
                                // TODO: Might have to refactor
                                INCOMING_CALL -> {
                                    PrefManager.setIncomingCallId(IncomingCallData.callId)
                                    val data = IncomingCall(callId = IncomingCallData.callId)
                                    mediator.showIncomingCall(data)
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
        voipLog?.log("Listining PSTN")
        ioScope.launch {
            try {
                pstnController.observePSTNState().collect {
                    try{
                        when (it) {
                            PSTNState.Idle -> {
                                mediator.userAction(Action.UNHOLD)
                            }
                            PSTNState.OnCall, PSTNState.Ringing -> {
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
            }
        }
    }

    /**
     * Events Which Repository can Use --- Start
     */
    fun connectCall(callData: HashMap<String, Any>) {
        if (callData != null) {
            mediator.connectCall(PEER_TO_PEER, callData)
            voipLog?.log("Connecting Call Data --> $callData")
        } else
            voipLog?.log("Mediator is NULL")
    }

    fun disconnectCall() {
        notification.idle()
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

class TestNotification : NotificationData {
    override fun setTitle(): String {
        return "Josh Skills"
    }

    override fun setContent(): String {
        return "Quickly Learn English"
    }
}

data class UIState(
    val remoteUserName: String,
    val remoteUserImage: String?,
    val topicName: String,
    val callType: Int,
    val isOnHold: Boolean = false,
    val isSpeakerOn: Boolean = false,
    val isRemoteUserMuted: Boolean = false,
    val isOnMute: Boolean = false,
    val isReconnecting: Boolean = false,
    val startTime: Long = 0L
) {
    companion object {
        fun empty() = UIState("", null, "", 0)
    }
}

enum class ServiceEvents {
    CALL_INITIATED_EVENT,
    CALL_CONNECTED_EVENT,
    RECONNECTING_FAILED,
    CLOSE_CALL_SCREEN
}