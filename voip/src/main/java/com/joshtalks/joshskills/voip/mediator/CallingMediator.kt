package com.joshtalks.joshskills.voip.mediator

import android.os.Message
import android.util.Log
import com.joshtalks.joshskills.base.constants.INTENT_DATA_INCOMING_CALL_ID
import com.joshtalks.joshskills.base.constants.PEER_TO_PEER
import com.joshtalks.joshskills.voip.communication.fallback.FirebaseChannelService
import com.joshtalks.joshskills.voip.audiomanager.SOUND_TYPE_RINGTONE
import com.joshtalks.joshskills.voip.audiomanager.SoundManager
import com.joshtalks.joshskills.voip.calldetails.IncomingCallData
import com.joshtalks.joshskills.voip.communication.EventChannel
import com.joshtalks.joshskills.voip.communication.PubNubChannelService
import com.joshtalks.joshskills.voip.communication.PubnubState
import com.joshtalks.joshskills.voip.communication.PubnubState.*
import com.joshtalks.joshskills.voip.communication.PubnubState.CONNECTED
import com.joshtalks.joshskills.voip.communication.PubnubState.RECONNECTED
import com.joshtalks.joshskills.voip.communication.constants.ServerConstants
import com.joshtalks.joshskills.voip.communication.model.*
import com.joshtalks.joshskills.voip.constant.*
import com.joshtalks.joshskills.voip.data.ServiceEvents
import com.joshtalks.joshskills.voip.data.UIState
import com.joshtalks.joshskills.voip.data.local.PrefManager
import com.joshtalks.joshskills.voip.notification.NotificationPriority
import com.joshtalks.joshskills.voip.notification.VoipNotification
import com.joshtalks.joshskills.voip.state.CallContext
import com.joshtalks.joshskills.voip.voipLog
import com.joshtalks.joshskills.voip.webrtc.AgoraWebrtcService
import com.joshtalks.joshskills.voip.webrtc.CallState
import com.joshtalks.joshskills.voip.webrtc.Envelope
import com.joshtalks.joshskills.voip.webrtc.WebrtcService
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlin.Exception
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.collections.HashMap

const val PER_USER_TIMEOUT_IN_MILLIS = 10 * 1000L
private const val TAG = "CallingMediator"

class CallingMediator(val scope: CoroutineScope) : CallServiceMediator {
    private val webrtcService: WebrtcService by lazy {
        AgoraWebrtcService(scope)
    }
    private val networkEventChannel: EventChannel by lazy {
        PubNubChannelService(scope)
    }
    private val fallbackEventChannel: EventChannel by lazy {
        FirebaseChannelService(scope)
    }

    private var calling = PeerToPeerCalling()
    val flow by lazy {
        MutableSharedFlow<Message>(replay = 0)
    }
    val uiStateFlow = MutableStateFlow(UIState.empty())
    val uiTransitionFlow = MutableSharedFlow<ServiceEvents>(replay = 0)
    private val mutex = Mutex(false)
    private val incomingCallMutex = Mutex(false)
    private val soundManager by lazy { SoundManager(SOUND_TYPE_RINGTONE, 20000) }
    private lateinit var voipNotification: VoipNotification
    private var isShowingIncomingCall = false
    private val Communication?.hasMainEventChannelFailed: Boolean
        get() {
            return PrefManager.getLatestPubnubMessageTime() < (this?.getEventTime() ?: 0)
        }
    var callContext : CallContext? = null
    lateinit var stateChannel : Channel<Envelope<Event>>

    init {
        scope.launch {
            mutex.withLock {
                handleWebrtcEvent()
                handlePubnubEvent()
                handleFallbackEvents()
                observeChannelState()
            }
        }
    }


    override fun observerUIState(): StateFlow<UIState> {
        return uiStateFlow
    }

    // TODO: Can be removed
    override fun observerUITransition(): SharedFlow<ServiceEvents> {
        return uiTransitionFlow
    }

    override fun observeEvents(): SharedFlow<Message> {
        return flow
    }

    override fun connectCall(callType: Int, callData: HashMap<String, Any>) {
        Log.d(TAG, "connectCall: ")
        // TODO: IMPORTANT- We have to make sure that in any case this should not be called twice
        scope.launch {
            mutex.withLock {
                /**
                 * Using State Pattern
                 */
                if (this@CallingMediator::voipNotification.isInitialized) {
                    voipNotification.removeNotification()
                    stopAudio()
                }
                callContext?.destroyContext()
                stateChannel = Channel()
                callContext = CallContext(
                    callType = callType,
                    request = callData,
                    direction = callData.direction(),
                    mediator = this@CallingMediator
                )
                callContext?.connect()
            }
        }
    }

    fun sendEventToServer(data: OutgoingData) {
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

    override fun userAction(action: UserAction) {
        scope.launch {
            try {
                when (action) {
                    UserAction.BACK_PRESS -> {
                        callContext?.backPress()
                    }
                    UserAction.DISCONNECT -> {
                        callContext?.disconnect()
                    }
                    UserAction.MUTE -> {
                        val envelope = Envelope(Event.MUTE_REQUEST)
                        stateChannel.send(envelope)
                    }
                    UserAction.UNMUTE -> {
                        val msg = Message.obtain().apply {
                            what = UNMUTE_REQUEST
                        }
                        stateChannel.send(msg)
                    }
                    UserAction.HOLD -> {
                        val msg = Message.obtain().apply {
                            what = HOLD_REQUEST
                        }
                        stateChannel.send(msg)
                    }
                    UserAction.UNHOLD -> {
                        val msg = Message.obtain().apply {
                            what = UNHOLD_REQUEST
                        }
                        stateChannel.send(msg)
                    }
                    UserAction.SPEAKER_ON -> {
                        val msg = Message.obtain().apply {
                            what = SPEAKER_ON_REQUEST
                        }
                        stateChannel.send(msg)
                    }
                    UserAction.SPEAKER_OFF -> {
                        val msg = Message.obtain().apply {
                            what = SPEAKER_OFF_REQUEST
                        }
                        stateChannel.send(msg)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun observeChannelState() {
        scope.launch {
            try {
                networkEventChannel.observeChannelState().collect {
                    when (it) {
                        CONNECTED -> {
                        }
                        RECONNECTED -> {
                            val msg = Message.obtain().apply {
                                what = SYNC_UI_STATE
                            }
                            stateChannel.send(msg)
                        }
                        DISCONNECTED -> {
                        }
                    }
                }
            } catch (e: Exception) {

            }
        }
    }

    override fun onDestroy() {
        networkEventChannel.onDestroy()
        fallbackEventChannel.onDestroy()
        webrtcService.onDestroy()
    }

    // Handle Events coming from Backend
    private fun handlePubnubEvent() {
        scope.launch {
            try {
                networkEventChannel.observeChannelEvents().collect {
                    val latestEventTimestamp = it.getEventTime() ?: 0L
                    PrefManager.setLatestPubnubMessageTime(latestEventTimestamp)
                    when (it) {
                        is Error -> {
                            callContext?.onError()
                        }
                        is ChannelData -> {
                            val msg = Message.obtain().apply {
                                what = RECEIVED_CHANNEL_DATA
                                obj = it
                            }
                            stateChannel.send(msg)
                        }
                        is MessageData -> {
                            if (isMessageForSameChannel(it.getChannel())) {
                                when (it.getType()) {
                                    ServerConstants.ONHOLD -> {
                                        val msg = Message.obtain().apply {
                                            what = HOLD
                                        }
                                        stateChannel.send(msg)
                                    }
                                    ServerConstants.RESUME -> {
                                        val msg = Message.obtain().apply {
                                            what = UNHOLD
                                        }
                                        stateChannel.send(msg)
                                    }
                                    ServerConstants.MUTE -> {
                                        val msg = Message.obtain().apply {
                                            what = MUTE
                                        }
                                        stateChannel.send(msg)
                                    }
                                    ServerConstants.UNMUTE -> {
                                        val msg = Message.obtain().apply {
                                            what = UNMUTE
                                        }
                                        stateChannel.send(msg)
                                    }
                                    ServerConstants.DISCONNECTED -> {
                                        val msg = Message.obtain().apply {
                                            what = REMOTE_USER_DISCONNECTED_MESSAGE
                                        }
                                        stateChannel.send(msg)
                                    }
                                }
                            }
                        }
                        is IncomingCall -> {
                            if (isShowingIncomingCall.not() && PrefManager.getVoipState() == State.IDLE) {
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
                                stateChannel.send(msg)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun handleWebrtcEvent() {
        scope.launch {
            try {
                webrtcService.observeCallingEvents().collect {
                    when (it) {
                        CallState.CallConnected -> {
                            // Call Connected
                            val msg = Message.obtain().apply {
                                what = CALL_CONNECTED_EVENT
                            }
                            stateChannel.send(msg)
                        }
                        CallState.CallDisconnected -> {
                            val msg = Message.obtain().apply {
                                what = CALL_DISCONNECTED
                            }
                            stateChannel.send(msg)
                        }
                        CallState.CallInitiated -> {
                            // CallInitiated
                            val msg = Message.obtain().apply {
                                what = CALL_INITIATED_EVENT
                            }
                            stateChannel.send(msg)
                        }
                        CallState.OnReconnected -> {
                            val msg = Message.obtain().apply {
                                what = com.joshtalks.joshskills.voip.constant.RECONNECTED
                            }
                            stateChannel.send(msg)
                        }
                        CallState.OnReconnecting -> {
                            val msg = Message.obtain().apply {
                                what = RECONNECTING
                            }
                            stateChannel.send(msg)
                        }
                        CallState.Error -> {
                            callContext?.onError()
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun handleFallbackEvents() {
        scope.launch {
            fallbackEventChannel.observeChannelEvents().collect { event ->
                if (event.hasMainEventChannelFailed) {
                    networkEventChannel.reconnect()
                    when (event) {
                        is Error -> {callContext?.onError()}
                        is ChannelData -> {
                            val msg = Message.obtain().apply {
                                what = RECEIVED_CHANNEL_DATA
                                obj = event
                            }
                            stateChannel.send(msg)
                        }
                        is MessageData -> {
                            if (isMessageForSameChannel(event.getChannel())) {
                                when (event.getType()) {
                                    ServerConstants.ONHOLD -> {
                                        // Transfer to Service
                                        val msg = Message.obtain().apply {
                                            what = HOLD
                                        }
                                        stateChannel.send(msg)
                                    }
                                    ServerConstants.RESUME -> {
                                        val msg = Message.obtain().apply {
                                            what = UNHOLD
                                        }
                                        stateChannel.send(msg)
                                    }
                                    ServerConstants.MUTE -> {
                                        val msg = Message.obtain().apply {
                                            what = MUTE
                                        }
                                        stateChannel.send(msg)
                                    }
                                    ServerConstants.UNMUTE -> {
                                        val msg = Message.obtain().apply {
                                            what = UNMUTE
                                        }
                                        stateChannel.send(msg)
                                    }
                                    // Remote User Disconnected
                                    ServerConstants.DISCONNECTED -> {
                                        val msg = Message.obtain().apply {
                                            what = REMOTE_USER_DISCONNECTED_MESSAGE
                                        }
                                        stateChannel.send(msg)
                                    }
                                }
                            }
                        }
                        is IncomingCall -> {
                            if (isShowingIncomingCall.not() && PrefManager.getVoipState() == State.IDLE) {
                                updateIncomingCallState(true)
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
                                stateChannel.send(msg)
                            }
                        }
                    }
                }
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

    private fun isMessageForSameChannel(channel: String) =
        channel == callContext?.channelData?.getChannel()

    private fun stopAudio() {
        try {
            soundManager.stopSound()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // TODO: Change Name
    fun disconnectCallFromWebrtc() {
        webrtcService.disconnectCall()
    }

    fun changeSpeaker() {

    }

    private fun HashMap<String, Any>.direction(): CallDirection {
        return if (get(INTENT_DATA_INCOMING_CALL_ID) != null)
            return CallDirection.INCOMING
        else
            CallDirection.OUTGOING
    }

    fun muteAudio(muteAudio: Boolean) {
        webrtcService.muteAudioStream(muteAudio)
    }

    fun joinChannel(channel: ChannelData) {
        val request = PeerToPeerCallRequest(
            channelName = channel.getChannel(),
            callToken = channel.getCallingToken(),
            agoraUId = channel.getAgoraUid()
        )
        webrtcService.connectCall(request)
    }
}