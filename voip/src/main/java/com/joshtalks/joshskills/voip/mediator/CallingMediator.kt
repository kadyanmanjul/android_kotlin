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
import kotlinx.coroutines.flow.*
import kotlin.Exception
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
        MutableSharedFlow<Envelope<Event>>(replay = 0)
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

    override fun observeEvents(): SharedFlow<Envelope<Event>> {
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
                        val envelope = Envelope(Event.UNMUTE_REQUEST)
                        stateChannel.send(envelope)
                    }
                    UserAction.HOLD -> {
                        val envelope = Envelope(Event.HOLD_REQUEST)
                        stateChannel.send(envelope)
                    }
                    UserAction.UNHOLD -> {
                        val envelope = Envelope(Event.UNHOLD_REQUEST)
                        stateChannel.send(envelope)
                    }
                    UserAction.SPEAKER_ON -> {
                        val envelope = Envelope(Event.SPEAKER_ON_REQUEST)
                        stateChannel.send(envelope)
                    }
                    UserAction.SPEAKER_OFF -> {
                        val envelope = Envelope(Event.SPEAKER_OFF_REQUEST)
                        stateChannel.send(envelope)
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
                        CONNECTED -> {}
                        RECONNECTED -> {
                            val envelope = Envelope(Event.SYNC_UI_STATE)
                            stateChannel.send(envelope)
                        }
                        DISCONNECTED -> {}
                    }
                }
            } catch (e: Exception) { }
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
                            val envelope = Envelope(Event.RECEIVED_CHANNEL_DATA,it)
                            stateChannel.send(envelope)
                        }
                        is MessageData -> {
                            if (isMessageForSameChannel(it.getChannel())) {
                                when (it.getType()) {
                                    ServerConstants.ONHOLD -> {
                                        val envelope = Envelope(Event.HOLD)
                                        stateChannel.send(envelope)
                                    }
                                    ServerConstants.RESUME -> {
                                        val envelope = Envelope(Event.UNHOLD)
                                        stateChannel.send(envelope)
                                    }
                                    ServerConstants.MUTE -> {
                                        val envelope = Envelope(Event.MUTE)
                                        stateChannel.send(envelope)
                                    }
                                    ServerConstants.UNMUTE -> {
                                        val envelope = Envelope(Event.UNMUTE)
                                        stateChannel.send(envelope)
                                    }
                                    ServerConstants.DISCONNECTED -> {
                                        val envelope = Envelope(Event.REMOTE_USER_DISCONNECTED_MESSAGE)
                                        stateChannel.send(envelope)
                                    }
                                }
                            }
                        }
                        is IncomingCall -> {
                            if (isShowingIncomingCall.not() && PrefManager.getVoipState() == State.IDLE) {
                                updateIncomingCallState(true)
                                voipLog?.log("Incoming Call -> $it")
                                IncomingCallData.set(it.getCallId(), PEER_TO_PEER)
                                val envelope = Envelope(Event.INCOMING_CALL)
                                flow.emit(envelope)
                            }
                        }
                        is UI -> {
                            if (isMessageForSameChannel(it.getChannelName())) {
                                val envelope = Envelope(Event.UI_STATE_UPDATED,it)
                                stateChannel.send(envelope)
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
                            val envelope = Envelope(Event.CALL_CONNECTED_EVENT)
                            stateChannel.send(envelope)
                        }
                        CallState.CallDisconnected -> {
                            val envelope = Envelope(Event.CALL_DISCONNECTED)
                            stateChannel.send(envelope)
                        }
                        CallState.CallInitiated -> {
                            // CallInitiated
                            val envelope = Envelope(Event.CALL_INITIATED_EVENT)
                            stateChannel.send(envelope)
                        }
                        CallState.OnReconnected -> {
                            val envelope = Envelope(Event.RECONNECTED)
                            stateChannel.send(envelope)
                        }
                        CallState.OnReconnecting -> {
                            val envelope = Envelope(Event.RECONNECTING)
                            stateChannel.send(envelope)
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
                            val envelope = Envelope(Event.RECEIVED_CHANNEL_DATA,event)
                            stateChannel.send(envelope)
                        }
                        is MessageData -> {
                            if (isMessageForSameChannel(event.getChannel())) {
                                when (event.getType()) {
                                    ServerConstants.ONHOLD -> {
                                        // Transfer to Service
                                        val envelope = Envelope(Event.HOLD)
                                        stateChannel.send(envelope)
                                    }
                                    ServerConstants.RESUME -> {
                                        val envelope = Envelope(Event.UNHOLD)
                                        stateChannel.send(envelope)
                                    }
                                    ServerConstants.MUTE -> {
                                        val envelope = Envelope(Event.MUTE)
                                        stateChannel.send(envelope)
                                    }
                                    ServerConstants.UNMUTE -> {
                                        val envelope = Envelope(Event.UNMUTE)
                                        stateChannel.send(envelope)
                                    }
                                    // Remote User Disconnected
                                    ServerConstants.DISCONNECTED -> {
                                        val envelope = Envelope(Event.REMOTE_USER_DISCONNECTED_MESSAGE)
                                        stateChannel.send(envelope)
                                    }
                                }
                            }
                        }
                        is IncomingCall -> {
                            if (isShowingIncomingCall.not() && PrefManager.getVoipState() == State.IDLE) {
                                updateIncomingCallState(true)
                                IncomingCallData.set(event.getCallId(), PEER_TO_PEER)
                                val envelope = Envelope(Event.INCOMING_CALL,event)
                                flow.emit(envelope)
                            }
                        }
                        is UI -> {
                            if (isMessageForSameChannel(event.getChannelName())) {
                                val envelope = Envelope(Event.UI_STATE_UPDATED,event)
                                stateChannel.send(envelope)
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