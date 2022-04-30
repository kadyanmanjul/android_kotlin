package com.joshtalks.joshskills.voip.mediator

import android.os.Message
import android.util.Log
import com.joshtalks.joshskills.base.constants.INTENT_DATA_INCOMING_CALL_ID
import com.joshtalks.joshskills.base.constants.PEER_TO_PEER
import com.joshtalks.joshskills.voip.communication.fallback.FirebaseChannelService
import com.joshtalks.joshskills.voip.Utils
import com.joshtalks.joshskills.voip.audiomanager.SOUND_TYPE_RINGTONE
import com.joshtalks.joshskills.voip.audiomanager.SoundManager
import com.joshtalks.joshskills.voip.calldetails.CallDetails
import com.joshtalks.joshskills.voip.calldetails.IncomingCallData
import com.joshtalks.joshskills.voip.communication.EventChannel
import com.joshtalks.joshskills.voip.communication.PubNubChannelService
import com.joshtalks.joshskills.voip.communication.PubnubState
import com.joshtalks.joshskills.voip.communication.constants.ServerConstants
import com.joshtalks.joshskills.voip.communication.model.*
import com.joshtalks.joshskills.voip.constant.*
import com.joshtalks.joshskills.voip.data.local.PrefManager
import com.joshtalks.joshskills.voip.notification.NotificationPriority
import com.joshtalks.joshskills.voip.notification.VoipNotification
import com.joshtalks.joshskills.voip.voipLog
import com.joshtalks.joshskills.voip.webrtc.AgoraWebrtcService
import com.joshtalks.joshskills.voip.webrtc.CallState
import com.joshtalks.joshskills.voip.webrtc.WebrtcService
import kotlinx.coroutines.*
import kotlin.Exception
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.collections.HashMap

const val PER_USER_TIMEOUT_IN_MILLIS = 10 * 1000L
const val CALL_CONNECT_TIMEOUT_IN_MILLIS = 2 * 60 * 1000L
private const val TAG = "CallingMediator"

class CallingMediator(val scope: CoroutineScope) : CallServiceMediator {
    private val webrtcService: WebrtcService by lazy {
        Log.d(TAG, "Creating : AgoraWebrtcService")
        AgoraWebrtcService(scope)
    }
    private val networkEventChannel: EventChannel by lazy {
        Log.d(TAG, "Creating : networkEventChannel")
        PubNubChannelService(scope)
    }
    private val fallbackEventChannel: EventChannel by lazy {
        Log.d(TAG, "Creating : fallbackEventChannel")
        FirebaseChannelService(scope)
    }
    private lateinit var callDirection: CallDirection
    private var calling = PeerToPeerCalling()
    private var callType = 0
    private val flow by lazy {
        Log.d(TAG, "Creating : flow")
        MutableSharedFlow<Message>(replay = 0)
    }
    private val mutex = Mutex(false)
    private val incomingCallMutex = Mutex(false)
    private val soundManager by lazy { SoundManager(SOUND_TYPE_RINGTONE, 20000) }
    private lateinit var voipNotification: VoipNotification
    private lateinit var userNotFoundJob: Job
    private var isShowingIncomingCall = false
    private val Communication?.hasMainEventChannelFailed: Boolean
        get() {
            return PrefManager.getLatestPubnubMessageTime() < (this?.getEventTime() ?: 0)
        }
    //var voipState : VoipState = IdleState(this)

    init {
        scope.launch {
            mutex.withLock {
                Log.d(TAG, " INIT : LOCK")
                handleWebrtcEvent()
                handlePubnubEvent()
                handleFallbackEvents()
                Log.d(TAG, " INIT : UNLOCK")
                Log.d(TAG, "Webrtc : ${webrtcService.hashCode()}")
                Log.d(TAG, "networkEventChannel : ${networkEventChannel.hashCode()}")
                Log.d(TAG, "fallbackEventChannel : ${fallbackEventChannel.hashCode()}")
                Log.d(TAG, "flow : ${flow.hashCode()}")
            }
        }
    }

    override fun observeEvents(): SharedFlow<Message> {
        return flow
    }

    private fun stopAudio() {
        try {
            soundManager.stopSound()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun connectCall(callType: Int, callData: HashMap<String, Any>) {
        Log.d(TAG, "connectCall: ")
        scope.launch {
            mutex.withLock {
                voipLog?.log("CallData Before Mutex --> $callData")
                try {
                    // Saving this to set Calltype when we receive Channel details
                    this@CallingMediator.callType = callType
                    // TODO: Need to Handle when Error Occurred
                    voipLog?.log("Coroutine CallData --> $callData")
                    if (this@CallingMediator::voipNotification.isInitialized) {
                        voipNotification.removeNotification()
                        stopAudio()
                    }

                    // TODO: Need to Fix Checking Incoming call two time
                    stopUserNotFoundTimer()
                    if (callData.isIncomingCall().not())
                        startUserNotFoundTimer()
                    calling.onPreCallConnect(callData)
                } catch (e: Exception) {
                    val msg = Message.obtain().apply {
                        what = ERROR
                    }
                    flow.emit(msg)
                    voipLog?.log("Connect Call API Failed")
                    e.printStackTrace()
                }
            }
        }
    }

    private fun HashMap<String, Any>.isIncomingCall(): Boolean {
        return get(INTENT_DATA_INCOMING_CALL_ID) != null
    }

    private fun handleFallbackEvents() {
        Log.d(TAG, "handleFallbackEvents: ${fallbackEventChannel.hashCode()}")
        Log.d(TAG, "handleFallbackEvents: Pubnub Channel Failed...")
        scope.launch {
            fallbackEventChannel.observeChannelEvents().collect { event ->
                if (event.hasMainEventChannelFailed) {
                    networkEventChannel.reconnect()
                    when (event) {
                        is Error -> {
                            /**
                             * Reset Webrtc
                             */
                            voipLog?.log("Error --> $event")
                            // TODO: Should not handle this here
                            Log.d("disconnectCall()", "handleFallbackEvents: ERROR")
                            webrtcService.disconnectCall()
                            val msg = android.os.Message.obtain().apply {
                                what = event.errorType
                            }
                            flow.emit(msg)
                        }
                        is ChannelData -> {
                            /**
                             * Join Channel
                             */
                            stopUserNotFoundTimer()
                            voipLog?.log("Channel Data -> ${event}")
                            // TODO: Use Calling Service type
                            val request = PeerToPeerCallRequest(
                                channelName = event.getChannel(),
                                callToken = event.getCallingToken(),
                                agoraUId = event.getAgoraUid()
                            )
                            webrtcService.connectCall(request)
                            CallDetails.reset()
                            CallDetails.set(event, callType)
                            val msg = Message.obtain().apply {
                                obj = event
                                what = RECEIVED_CHANNEL_DATA
                            }
                            flow.emit(msg)
                        }
                        is MessageData -> {
                            voipLog?.log("Message Data -> $event")
                            Log.d(TAG, "handleFallbackEvents: $event")
                            if (isMessageForSameChannel(event.getChannel())) {
                                when (event.getType()) {
                                    ServerConstants.ONHOLD -> {
                                        // Transfer to Service
                                        val msg = Message.obtain().apply {
                                            what = HOLD
                                        }
                                        flow.emit(msg)
                                    }
                                    ServerConstants.RESUME -> {
                                        val msg = Message.obtain().apply {
                                            what = UNHOLD
                                        }
                                        flow.emit(msg)
                                    }
                                    ServerConstants.MUTE -> {
                                        val msg = Message.obtain().apply {
                                            what = MUTE
                                        }
                                        flow.emit(msg)
                                    }
                                    ServerConstants.UNMUTE -> {
                                        val msg = Message.obtain().apply {
                                            what = UNMUTE
                                        }
                                        flow.emit(msg)
                                    }
                                    // Remote User Disconnected
                                    ServerConstants.DISCONNECTED -> {
                                        Log.d("disconnectCall()", "handleFallbackEvents: DISCO")
                                        webrtcService.disconnectCall()
                                        val msg = Message.obtain().apply {
                                            what = CALL_DISCONNECT_REQUEST
                                        }
                                        flow.emit(msg)
                                    }
                                }
                            }
                        }
                        is IncomingCall -> {
                            if (isShowingIncomingCall.not()) {
                                updateIncomingCallState(true)
                                voipLog?.log("Incoming Call -> $event")
                                IncomingCallData.set(event.getCallId(), PEER_TO_PEER)
                                val msg = Message.obtain().apply {
                                    what = INCOMING_CALL
                                    obj = event
                                }
                                flow.emit(msg)
                            }
                        }
                        is UI -> {
                            if (isMessageForSameChannel(event.getChannelName())) {
                                val msg = Message.obtain().apply {
                                    obj = event
                                    what = UI_STATE_UPDATED
                                }
                                flow.emit(msg)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun startUserNotFoundTimer() {
        userNotFoundJob = scope.launch {
            ensureActive()
            val timeout = Timeout(ServerConstants.TIMEOUT)
            for (i in 1..12) {
                delay(PER_USER_TIMEOUT_IN_MILLIS)
                ensureActive()
                networkEventChannel.emitEvent(timeout)
                Log.d(TAG, "startUserNotFoundTimer: $i")
            }
            Log.d(TAG, "startUserNotFoundTimer: Sending Disconnect")
            delay(PER_USER_TIMEOUT_IN_MILLIS)
            ensureActive()
            noUserFound()
            Log.d(TAG, "startUserNotFoundTimer: disconnectCall()")
            disconnectCall()
            Log.d(TAG, "startUserNotFoundTimer: CALL_DISCONNECT_REQUEST")
            scope.launch {
                val msg = Message.obtain().apply {
                    what = CALL_DISCONNECT_REQUEST
                }
                flow.emit(msg)
            }
        }
    }

    private fun noUserFound() {
        val noUserFound = UserAction(
            channelName = "NO_CHANNEL",
            type = ServerConstants.NO_USER_FOUND,
            address = Utils.uuid ?: ""
        )
        networkEventChannel.emitEvent(noUserFound)
    }

    private fun stopUserNotFoundTimer() {
        Log.d(TAG, "stopUserNotFoundTimer: ")
        if (::userNotFoundJob.isInitialized) {
            Log.d(TAG, "stopUserNotFoundTimer: Cancelling")
            userNotFoundJob.cancel()
        }
    }

    override fun muteAudioStream(muteAudio: Boolean) {
        webrtcService.muteAudioStream(muteAudio)
    }

    override fun sendEventToServer(data: OutgoingData) {
        networkEventChannel.emitEvent(data)
    }

    override fun showIncomingCall(incomingCall: IncomingCall) {
        showIncomingNotification(incomingCall)
    }

    override fun hideIncomingCall() {
        scope.launch {
            try {
                val map = HashMap<String, Any>(1).apply {
                    put(INTENT_DATA_INCOMING_CALL_ID, IncomingCallData.callId)
                }
                calling.onCallDecline(map)
                stopAudio()
                voipNotification.removeNotification()
                updateIncomingCallState(false)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun switchAudio() {}

    override fun disconnectCall() {
        voipLog?.log("Disconnect Call")
        stopUserNotFoundTimer()
        Log.d("disconnectCall()", "disconnectCall: ")
        webrtcService.disconnectCall()
    }

    override fun observeState(): SharedFlow<Int> {
        return webrtcService.observeCallingState()
    }

    override fun observeChannelState(): SharedFlow<PubnubState> {
        return networkEventChannel.observeChannelState()
    }

    // Handle Events coming from Backend
    private fun handlePubnubEvent() {
        Log.d(TAG, "handlePubnubEvent: ${networkEventChannel.hashCode()}")
        scope.launch {
            try {
                networkEventChannel.observeChannelEvents().collect {
                    val latestEventTimestamp = it.getEventTime() ?: 0L
                    PrefManager.setLatestPubnubMessageTime(latestEventTimestamp)
                    Log.d(TAG, "handlePubnubEvent: ${this.hashCode()} .... ${networkEventChannel.hashCode()}")
                    when (it) {
                        is Error -> {
                            /**
                             * Reset Webrtc
                             */
                            voipLog?.log("Error --> $it")
                            // TODO: Should not handle this here
                            Log.d("disconnectCall()", "handlePubnubEvent: ERROR")
                            webrtcService.disconnectCall()
                            val msg = Message.obtain().apply {
                                what = it.errorType
                            }
                            flow.emit(msg)
                        }
                        is ChannelData -> {
                            /**
                             * Join Channel
                             */
                            stopUserNotFoundTimer()
                            voipLog?.log("Channel Data -> ${it}")
                            // TODO: Use Calling Service type
                            val request = PeerToPeerCallRequest(
                                channelName = it.getChannel(),
                                callToken = it.getCallingToken(),
                                agoraUId = it.getAgoraUid()
                            )
                            webrtcService.connectCall(request)
                            CallDetails.reset()
                            CallDetails.set(it, callType)
                            Log.d("disconnectCall()", "handlePubnubEvent: Channel Data")
                            //webrtcService.disconnectCall()
                            val msg = Message.obtain().apply {
                                what = RECEIVED_CHANNEL_DATA
                                obj = it
                            }
                            flow.emit(msg)
                        }
                        is MessageData -> {
                            voipLog?.log("Message Data -> $it")
                            Log.d(TAG, "handlePubnubEvent: $it")
                            if (isMessageForSameChannel(it.getChannel())) {
                                when (it.getType()) {
                                    ServerConstants.ONHOLD -> {
                                        // Transfer to Service
                                        val msg = Message.obtain().apply {
                                            what = HOLD
                                        }
                                        flow.emit(msg)
                                    }
                                    ServerConstants.RESUME -> {
                                        val msg = Message.obtain().apply {
                                            what = UNHOLD
                                        }
                                        flow.emit(msg)
                                    }
                                    ServerConstants.MUTE -> {
                                        val msg = Message.obtain().apply {
                                            what = MUTE
                                        }
                                        flow.emit(msg)
                                    }
                                    ServerConstants.UNMUTE -> {
                                        val msg = Message.obtain().apply {
                                            what = UNMUTE
                                        }
                                        flow.emit(msg)
                                    }
                                    ServerConstants.DISCONNECTED -> {
                                        Log.d("disconnectCall()", "handlePubnubEvent: DISCO")
                                        webrtcService.disconnectCall()
                                        val msg = Message.obtain().apply {
                                            what = CALL_DISCONNECT_REQUEST
                                        }
                                        flow.emit(msg)
                                    }
                                }
                            }
                        }
                        is IncomingCall -> {
                            if (isShowingIncomingCall.not()) {
                                updateIncomingCallState(true)
                                voipLog?.log("Incoming Call -> $it")
                                IncomingCallData.set(it.getCallId(), PEER_TO_PEER)
                                val msg = Message.obtain().apply {
                                    what = INCOMING_CALL
                                }
                                flow.emit(msg)
                            }
                        }
                        is UI -> {
                            if (isMessageForSameChannel(it.getChannelName())) {
                                val msg = Message.obtain().apply {
                                    obj = it
                                    what = UI_STATE_UPDATED
                                }
                                flow.emit(msg)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun showIncomingNotification(incomingCall: IncomingCall) {
        val remoteView =
            calling.notificationLayout(incomingCall) ?: return // TODO: might throw error
        voipNotification = VoipNotification(remoteView, NotificationPriority.High)
        voipNotification.show()
        soundManager.playSound()
        scope.launch {
            delay(20000)
            voipNotification.removeNotification()
            updateIncomingCallState(false)
            stopAudio()
        }
    }

    private fun updateIncomingCallState(isShowingIncomingCall: Boolean) {
        scope.launch {
            incomingCallMutex.withLock {
                this@CallingMediator.isShowingIncomingCall = isShowingIncomingCall
            }
        }
    }

    private fun isMessageForSameChannel(channel : String) =
        channel == CallDetails.agoraChannelName

    private fun handleWebrtcEvent() {
        Log.d(TAG, "handleWebrtcEvent: ${webrtcService.hashCode()}")
        scope.launch {
            try {
                webrtcService.observeCallingEvents().collect {
                    when (it) {
                        CallState.CallConnected -> {
                            // Call Connected
                            val msg = Message.obtain().apply {
                                what = CALL_CONNECTED_EVENT
                            }
                            flow.emit(msg)
                            voipLog?.log("Call Connected")
                        }
                        CallState.CallDisconnected -> {
                            val msg = Message.obtain().apply {
                                what = CALL_DISCONNECTED
                            }
                            flow.emit(msg)
                            voipLog?.log("Call Disconnected")
                        }
                        CallState.ReconnectingFailed -> {
                            val msg = Message.obtain().apply {
                                what = RECONNECTING_FAILED
                            }
                            flow.emit(msg)
                            voipLog?.log("Call Disconnect Request")
                        }
                        CallState.CallInitiated -> {
                            // CallInitiated
                            val msg = Message.obtain().apply {
                                what = CALL_INITIATED_EVENT
                            }
                            flow.emit(msg)
                            voipLog?.log("Call CallInitiated")
                        }
                        CallState.OnReconnected -> {
                            val msg = Message.obtain().apply {
                                what = RECONNECTED
                            }
                            flow.emit(msg)
                            voipLog?.log("OnReconnected")
                        }

                        CallState.OnReconnecting -> {
                            val msg = Message.obtain().apply {
                                what = RECONNECTING
                            }
                            flow.emit(msg)
                            voipLog?.log("OnReconnecting")
                        }
                        CallState.Error -> {
                            val msg = Message.obtain().apply {
                                what = ERROR
                            }
                            flow.emit(msg)
                            voipLog?.log("Error")
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onDestroy() {
        networkEventChannel.onDestroy()
        fallbackEventChannel.onDestroy()
        webrtcService.onDestroy()
    }
}

enum class CallDirection {
    INCOMING,
    OUTGOING
}