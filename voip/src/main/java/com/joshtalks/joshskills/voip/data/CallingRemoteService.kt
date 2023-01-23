package com.joshtalks.joshskills.voip.data

import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import com.joshtalks.joshskills.base.constants.*
import com.joshtalks.joshskills.voip.*
import com.joshtalks.joshskills.voip.constant.*
import com.joshtalks.joshskills.voip.constant.Event.CALL_CONNECTED_EVENT
import com.joshtalks.joshskills.voip.constant.Event.CALL_INITIATED_EVENT
import com.joshtalks.joshskills.voip.constant.Event.CLOSE_CALL_SCREEN
import com.joshtalks.joshskills.voip.constant.Event.RECONNECTING_FAILED
import com.joshtalks.joshskills.voip.data.local.PrefManager
import com.joshtalks.joshskills.voip.mediator.CallServiceMediator
import com.joshtalks.joshskills.voip.mediator.CallingMediator
import com.joshtalks.joshskills.voip.notification.NotificationData
import com.joshtalks.joshskills.voip.notification.NotificationPriority
import com.joshtalks.joshskills.voip.notification.VoipNotification
import com.joshtalks.joshskills.voip.pstn.PSTNController
import com.joshtalks.joshskills.voip.pstn.PSTNState
import com.joshtalks.joshskills.voip.state.CallConnectData
import com.joshtalks.joshskills.voip.voipanalytics.CallAnalytics
import com.joshtalks.joshskills.voip.voipanalytics.EventName
import com.joshtalks.joshskills.voip.webrtc.Envelope
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import timber.log.Timber
import com.joshtalks.joshskills.base.model.NotificationData as Data
import com.joshtalks.joshskills.voip.mediator.UserAction as Action

private const val TAG = "CallingRemoteService"
const val SERVICE_ALONE_LIFE_TIME = 1 * 60 * 1000L

class CallingRemoteService : Service() {
    private var isServiceInitialize = false
    private val coroutineExceptionHandler = CoroutineExceptionHandler { _, e ->
        Timber.tag("Coroutine Exception").d("Handled...")
        e.printStackTrace()
    }
    private val ioScope by lazy { CoroutineScope(Dispatchers.IO + coroutineExceptionHandler) }
    private val destroyScope by lazy { CoroutineScope(Dispatchers.IO + coroutineExceptionHandler) }
    private val syncScope by lazy { CoroutineScope(Dispatchers.IO + coroutineExceptionHandler) }
    private val mediator by lazy<CallServiceMediator> { CallingMediator(ioScope) }
    private var isMediatorInitialise = false
    private val pstnController by lazy { PSTNController(ioScope) }
    private val serviceEvents = MutableSharedFlow<Envelope<ServiceEvents>>(replay = 0)

    // For Testing Purpose
    private val notificationData by lazy { TestNotification(getNotificationData()) }
    private val notification by lazy { VoipNotification(notificationData, NotificationPriority.Low) }
    private val binder = RemoteServiceBinder()
    private var timeInMillSec: Long? = null

    var countdownTimerBack: Job? = null
    var expertCallData = HashMap<String, Any>()
    private val timerScope by lazy { CoroutineScope(Dispatchers.IO + coroutineExceptionHandler) }

//    private var beepTimer: BeepTimer? = null

    override fun onCreate() {
        Log.d(TAG, "onCreate: ")
        super.onCreate()
        PrefManager.initServicePref(this)
        PrefManager.setVoipState(State.IDLE)
        updateStartTime(0)
        syncScope.launch {
            Utils.syncAnalytics()
            Utils.syncCallRecordingAudios()
        }
        registerReceivers()
        observerPstnService()
        showNotification()
        Log.d(TAG, "onCreate: Creating Service")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "StartService --- OnStartCommand")
        if(intent != null && intent.action != SERVICE_ACTION_STOP_SERVICE)
            PrefManager.voipServiceUsed()
        when(intent?.action) {
            // TODO: have to change
            SERVICE_ACTION_INCOMING_CALL -> {
                val map = HashMap<String, String>()
                map[INCOMING_CALL_CATEGORY] = intent.extras?.getString(com.joshtalks.joshskills.voip.constant.INCOMING_CALL_CATEGORY,"")?:""
                map[INTENT_DATA_INCOMING_CALL_ID] = intent.extras?.getString(com.joshtalks.joshskills.voip.constant.INCOMING_CALL_ID,"")?:""
                map[com.joshtalks.joshskills.voip.constant.REMOTE_USER_NAME] = intent.extras?.getString(com.joshtalks.joshskills.voip.constant.REMOTE_USER_NAME,"")?:""
                map[com.joshtalks.joshskills.voip.constant.INCOMING_GROUP_NAME] = intent.extras?.getString(com.joshtalks.joshskills.voip.constant.INCOMING_GROUP_NAME,"")?:""
                map[com.joshtalks.joshskills.voip.constant.INCOMING_GROUP_IMAGE] = intent.extras?.getString(com.joshtalks.joshskills.voip.constant.INCOMING_GROUP_IMAGE,"")?:""
                map[com.joshtalks.joshskills.voip.constant.IS_PREMIUM_USER] = intent.extras?.getString(com.joshtalks.joshskills.voip.constant.IS_PREMIUM_USER,"false") ?: "false"
                ioScope.launch { mediator.handleIncomingCall(map) }
            }
            SERVICE_ACTION_STOP_SERVICE -> {
                CoroutineScope(Dispatchers.IO).launch {
                    stopService()
                }
                return START_NOT_STICKY
            }
            ANALYTICS_EVENT ->{
                if(intent.extras?.getString("event","").equals("notification")){
                    CallAnalytics.addAnalytics(
                        event = EventName.CALL_RECORDING_NOTIFICATION_CLICKED,
                        agoraCallId = PrefManager.getAgraCallId().toString(),
                        agoraMentorId = PrefManager.getLocalUserAgoraId().toString()
                    )
                }else{
                    CallAnalytics.addAnalytics(
                        event = EventName.RECORDING_SHARE_BUTTON_CLICKED,
                        agoraCallId = PrefManager.getAgraCallId().toString(),
                        agoraMentorId = PrefManager.getLocalUserAgoraId().toString()
                    )
                }
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
            SERVICE_ACTION_INCOMING_CALL_HIDE->{
                mediator.hideIncomingCall()
            }
            SERVICE_ACTION_INCOMING_CALL_DECLINE -> {
                CallAnalytics.addAnalytics(
                    event = EventName.INCOMING_CALL_DECLINE,
                    agoraCallId ="-1",
                    agoraMentorId = PrefManager.getLocalUserAgoraId().toString()
                )
                mediator.hideIncomingCall()
                mediator.declineIncomingCall()

                return START_NOT_STICKY
            }
            START_EXPERT_CALL_TIMER ->{
            }

        }
        return if (isServiceInitialize)
            START_STICKY
        else
            intent.initService()
    }

    private suspend fun stopService() {
        if (PrefManager.getVoipState() == State.CONNECTED) {
            disconnectCall()
        }
        delay(5000)
        ioScope.cancel()
        syncScope.cancel()
        BeepTimer.stopBeepSound()
        stopSelf()
    }

    fun getUserDetails(): StateFlow<UIState> = mediator.observerUIState()

    fun getEvents(): SharedFlow<Envelope<ServiceEvents>> = serviceEvents

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
                                        intentOnNotificationTap(),
                                        getHangUpIntent()
                                    )
                                    serviceEvents.emit(Envelope(ServiceEvents.CALL_CONNECTED_EVENT))
                                    if (expertCallData[IS_EXPERT_CALLING] == "true") {
                                        startCallTimer()
                                    }
                                }
                                CLOSE_CALL_SCREEN -> {
                                    stopCallTimer()
                                    serviceEvents.emit(Envelope(ServiceEvents.CLOSE_CALL_SCREEN))
                                    notification.idle(getNotificationData())
                                }
                                RECONNECTING_FAILED -> {
                                    serviceEvents.emit(Envelope(ServiceEvents.RECONNECTING_FAILED))
                                    notification.idle(getNotificationData())
                                }
//                                // TODO: Might have to refactor
//                                INCOMING_CALL -> {
//                                    PrefManager.setIncomingCallId(IncomingCallData.callId)
//                                    val data = IncomingCall(callId = IncomingCallData.callId)
//                                    mediator.showIncomingCall(data)
//                                }
                                CALL_INITIATED_EVENT -> {
                                    serviceEvents.emit(Envelope(ServiceEvents.CALL_INITIATED_EVENT))
                                }
                                else -> {}
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
        PrefManager.voipServiceUsed()
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
                                mediator.hideIncomingCall()
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

    /**
     * Events Which Repository can Use --- Start
     */
    fun connectCall(callData: HashMap<String, Any>, category: Category = Category.PEER_TO_PEER) {
        if (callData != null) {
            mediator.connectCall(category, callData)
            notification.searching()
            expertCallData = callData
            Log.d(TAG, "Connecting Call Data --> $callData")
        } else
            Log.d(TAG, "connectCall: Call Data is Null")
    }

    fun disconnectCall() {
        BeepTimer.stopBeepSound()
        stopCallTimer()
        notification.idle(getNotificationData())
        mediator.userAction(Action.DISCONNECT)
    }

    fun changeMicState(isMicOn: Boolean) {
        mediator.userAction(if (isMicOn) Action.UNMUTE else Action.MUTE)
    }


    /**
     * 1. Connected State
     *
     *
     *
     * AudioRouteListener - Immutable
     * AudioController - Audio Switching
     */

    fun changeSpeakerState(isSpeakerOn: Boolean) {
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
        stopCallTimer()
        unregisterReceivers()
        destroyScope.launch {
            mediator.onDestroy()
            destroyScope.cancel()
            super.onDestroy()
        }
        ioScope.cancel()
        syncScope.cancel()
        BeepTimer.stopBeepSound()
    }

    private fun showNotification() {
        startForeground(
            notification.getNotificationId(),
            notification.getNotificationObject().build()
        )
    }

    private fun registerReceivers() {
        pstnController.registerPstnReceiver()
    }

    private fun unregisterReceivers() {
        pstnController.unregisterPstnReceiver()
    }

    fun startTimer(totalWalletAmount: Int, expertPrice: Int):Job? {
        try {
            timeInMillSec = (((totalWalletAmount / expertPrice) * 60) * 1000).toLong()
//            beepTimer = BeepTimer(this)
            countdownTimerBack = timerScope.launch {
                try {
                    delay(timeInMillSec!! - BeepTimer.TIMER_DURATION)
                    BeepTimer.startBeepSound(this@CallingRemoteService)
                    delay(BeepTimer.TIMER_DURATION)
                    disconnectCall()
                } catch (e: Exception){

                }
            }
        }catch (ex:Exception){
            stopCallTimer()
        }
        return countdownTimerBack
    }

    fun startCallTimer() {
        val isPremiumCall = expertCallData[INTENT_DATA_EXPERT_PREMIUM] as Boolean
        if (!isPremiumCall && (countdownTimerBack == null || countdownTimerBack?.isActive == false)) {
            countdownTimerBack = startTimer(
                Integer.parseInt(expertCallData[INTENT_DATA_TOTAL_AMOUNT].toString()),
                Integer.parseInt(expertCallData[INTENT_DATA_EXPERT_PRICE_PER_MIN].toString()),
            )
            countdownTimerBack?.start()
        }
    }

    private fun stopCallTimer() {
        countdownTimerBack?.cancel()
        countdownTimerBack = null
    }

}

// TODO: Need to Change
class TestNotification(val notiData : Data) : NotificationData {
    override fun setTitle(): String {
        return notiData.title.ifEmpty {
            "User, You will learn English by speaking."
        }
    }

    override fun setContent(): String {
        return notiData.body.ifEmpty {
            "Call Now"
        }
    }

    override fun setTapAction(): PendingIntent? {
        return openCallScreen()
    }
}

// TODO: Should be in a new Class
data class UIState(
    val remoteUserName: String,
    val remoteUserImage: String?,
    val topicName: String,
    val callType: Int,
    val currentTopicImage: String,
    val occupation: String,
    val aspiration: String,
    val interestHeader : String = "",
    val interests : List<String> = emptyList(),
    val isOnHold: Boolean = false,
    val isSpeakerOn: Boolean = false,
    val isRemoteUserMuted: Boolean = false,
    val isOnMute: Boolean = false,
    val isReconnecting: Boolean = false,
    val startTime: Long = 0L,
    ) {
    companion object {
        fun empty() = UIState("", null, "", 0,"","","")
    }
}

enum class ServiceEvents {
    CALL_INITIATED_EVENT,
    CALL_CONNECTED_EVENT,
    RECONNECTING_FAILED,
    CLOSE_CALL_SCREEN,
}