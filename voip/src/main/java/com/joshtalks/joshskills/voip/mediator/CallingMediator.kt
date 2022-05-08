package com.joshtalks.joshskills.voip.mediator

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
import com.joshtalks.joshskills.voip.voipanalytics.CallAnalytics
import com.joshtalks.joshskills.voip.voipanalytics.EventName
import com.joshtalks.joshskills.voip.webrtc.*
import com.joshtalks.joshskills.voip.webrtc.AgoraWebrtcService
import com.joshtalks.joshskills.voip.webrtc.CallState
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
        Log.d(TAG, "Inside Init : ${scope.isActive}")
        scope.launch {
            try{
                mutex.withLock {
                    handleWebrtcEvent()
                    handlePubnubEvent()
                    handleFallbackEvents()
                    observeChannelState()
                }
            }
            catch (e : Exception){
                if(e is CancellationException)
                    throw e
                e.printStackTrace()
            }
        }
    }


    override fun observerUIState(): StateFlow<UIState> {
        return uiStateFlow
    }

    override fun observerUITransition(): SharedFlow<ServiceEvents> {
        return uiTransitionFlow
    }

    override fun observeEvents(): SharedFlow<Envelope<Event>> {
        return flow
    }

    override fun connectCall(callType: Int, callData: HashMap<String, Any>) {
        Log.d(TAG, "connectCall: ${scope.isActive} ")
        // TODO: IMPORTANT- We have to make sure that in any case this should not be called twice
        scope.launch {
            Log.d(TAG, "connectCall : Inside Scope")
            try{
                mutex.withLock {
                    /**
                     * Using State Pattern
                     */
                    Log.d(TAG, "connectCall : Inside Lock")
                    if (this@CallingMediator::voipNotification.isInitialized) {
                        voipNotification.removeNotification()
                        stopAudio()
                    }
                    callContext?.destroyContext()
                    stateChannel = Channel(Channel.UNLIMITED)
                    callContext = CallContext(
                        callType = callType,
                        request = callData,
                        direction = callData.direction(),
                        mediator = this@CallingMediator
                    )
                    callContext?.connect()
                }
            }
            catch (e : Exception){
                if(e is CancellationException)
                    throw e
                e.printStackTrace()
            }
        }
    }

    fun sendEventToServer(data: OutgoingData) {
        Log.d(TAG, "sendEventToServer : $data")
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
                if(e is CancellationException)
                    throw e
            }
        }
    }

    override fun userAction(action: UserAction) {
        Log.d(TAG, "userAction : $action")
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
                        CallAnalytics.addAnalytics(
                            event = EventName.PSTN_CALL_RECEIVED,
                            agoraCallId = callContext?.channelData?.getCallingId().toString(),
                            agoraMentorId = callContext?.channelData?.getAgoraUid().toString()
                        )
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
                Log.d(TAG, "userAction : $e")
                e.printStackTrace()
                if(e is CancellationException)
                    throw e
            }
        }
    }

    private fun observeChannelState() {
        scope.launch {
            try {
                networkEventChannel.observeChannelState().collect {
                    try{
                        when (it) {
                            CONNECTED -> {  Log.d(TAG, "observeChannelState : $it") }
                            RECONNECTED -> {
                                Log.d(TAG, "observeChannelState : $it")
                                val envelope = Envelope(Event.SYNC_UI_STATE)
                                stateChannel.send(envelope)
                            }
                            DISCONNECTED -> { Log.d(TAG, "observeChannelState : $it") }
                        }
                    }
                    catch (e : Exception){
                        if(e is CancellationException)
                            throw e
                        e.printStackTrace()
                    }
                }
            } catch (e: Exception) {
                Log.d(TAG, "observeChannelState: $e")
                e.printStackTrace()
                if(e is CancellationException)
                    throw e
            }
        }
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy : Destroying channel and services")
        networkEventChannel.onDestroy()
        fallbackEventChannel.onDestroy()
        webrtcService.onDestroy()
    }

    // Handle Events coming from Backend
    private fun handlePubnubEvent() {
        Log.d(TAG, "handlePubnubEvent: Observe")
        scope.launch {
            try {
                networkEventChannel.observeChannelEvents().collect {
                    try{
                        Log.d(TAG, "handlePubnubEvent: Collect $it")
                        val latestEventTimestamp = it.getEventTime() ?: 0L
                        PrefManager.setLatestPubnubMessageTime(latestEventTimestamp)
                        when (it) {
                            is Error -> {
                                Log.d(TAG, "handlePubnubEvent : $it")
                                callContext?.onError()
                            }
                            is ChannelData -> {
                                Log.d(TAG, "handlePubnubEvent : $it")
                                val envelope = Envelope(Event.RECEIVED_CHANNEL_DATA,it)
                                stateChannel.send(envelope)
                            }
                            is MessageData -> {
                                Log.d(TAG, "handlePubnubEvent : $it")
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
                                            CallAnalytics.addAnalytics(
                                                event = EventName.DISCONNECTED_BY_REMOTE_USER,
                                                agoraCallId = callContext?.channelData?.getCallingId().toString(),
                                                agoraMentorId = callContext?.channelData?.getAgoraUid().toString()
                                            )
                                            val envelope = Envelope(Event.REMOTE_USER_DISCONNECTED_MESSAGE)
                                            stateChannel.send(envelope)
                                        }
                                    }
                                }
                            }
                            is IncomingCall -> {
                                Log.d(TAG, "handlePubnubEvent : $it")
                                if (isShowingIncomingCall.not() && PrefManager.getVoipState() == State.IDLE) {
                                    CallAnalytics.addAnalytics(
                                        event = EventName.INCOMING_CALL_RECEIVED,
                                        agoraCallId = callContext?.channelData?.getCallingId().toString(),
                                        agoraMentorId = callContext?.channelData?.getAgoraUid().toString()
                                    )
                                    updateIncomingCallState(true)
                                    voipLog?.log("Incoming Call -> $it")
                                    IncomingCallData.set(it.getCallId(), PEER_TO_PEER)
                                    val envelope = Envelope(Event.INCOMING_CALL)
                                    flow.emit(envelope)
                                }
                            }
                            is UI -> {
                                Log.d(TAG, "handlePubnubEvent : $it")
                                if (isMessageForSameChannel(it.getChannelName())) {
                                    val envelope = Envelope(Event.UI_STATE_UPDATED,it)
                                    stateChannel.send(envelope)
                                }
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
                Log.d(TAG, "handlePubnubEvent : $e")
                e.printStackTrace()
                if(e is CancellationException)
                    throw e
            }
        }
    }

    private fun handleWebrtcEvent() {
        scope.launch {
            try {
                webrtcService.observeCallingEvents().collect {
                    try{
                        when (it) {
                            CallState.CallConnected -> {
                                // Call Connected
                                Log.d(TAG, "handleWebrtcEvent : $it")
                                val envelope = Envelope(Event.CALL_CONNECTED_EVENT)
                                stateChannel.send(envelope)
                            }
                            CallState.CallDisconnected -> {
                                Log.d(TAG, "handleWebrtcEvent : $it")
                                val envelope = Envelope(Event.CALL_DISCONNECTED)
                                stateChannel.send(envelope)
                            }
                            CallState.CallInitiated -> {
                                // CallInitiated
                                Log.d(TAG, "handleWebrtcEvent : $it")
                                val envelope = Envelope(Event.CALL_INITIATED_EVENT)
                                stateChannel.send(envelope)
                            }
                            CallState.OnReconnected -> {
                                Log.d(TAG, "handleWebrtcEvent : $it")
                                val envelope = Envelope(Event.RECONNECTED)
                                stateChannel.send(envelope)
                            }
                            CallState.OnReconnecting -> {
                                Log.d(TAG, "handleWebrtcEvent : $it")
                                CallAnalytics.addAnalytics(
                                    event = EventName.CALL_RECONNECTING,
                                    agoraCallId = PrefManager.getAgraCallId().toString(),
                                    agoraMentorId = PrefManager.getLocalUserAgoraId().toString()
                                )
                                val envelope = Envelope(Event.RECONNECTING)
                                stateChannel.send(envelope)
                            }
                            CallState.Error -> {
                                Log.d(TAG, "handleWebrtcEvent : $it")
                                callContext?.onError()
                            }
                            CallState.UserLeftChannel -> {
                                val envelope = Envelope(Event.REMOTE_USER_DISCONNECTED_USER_LEFT)
                                stateChannel.send(envelope)
                            }
                        }
                    }
                    catch (e : Exception){
                        if(e is CancellationException)
                            throw e
                        e.printStackTrace()
                    }                }
            } catch (e: Exception) {
                Log.d(TAG, "handleWebrtcEvent : $e")
                e.printStackTrace()
                if(e is CancellationException)
                    throw e
            }
        }
    }

    private fun handleFallbackEvents() {
        scope.launch {
            try{
                fallbackEventChannel.observeChannelEvents().collect { event ->
                    try{
                        if (event.hasMainEventChannelFailed) {
                            Log.d(TAG, "handleFallbackEvents: Pubnub Listener Failed ...")
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
                                                CallAnalytics.addAnalytics(
                                                    event = EventName.DISCONNECTED_BY_REMOTE_USER,
                                                    agoraCallId = callContext?.channelData?.getCallingId().toString(),
                                                    agoraMentorId = callContext?.channelData?.getAgoraUid().toString()
                                                )
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
                    catch (e : Exception){
                        if(e is CancellationException)
                            throw e
                        e.printStackTrace()
                    }

                }
            }
            catch (e : Exception){
                if(e is CancellationException)
                    throw e
                e.printStackTrace()
            }

        }
    }

    private fun showIncomingNotification(incomingCall: IncomingCall) {
        val remoteView =
            calling.notificationLayout(incomingCall) ?: return // TODO: might throw error
        voipNotification = VoipNotification(remoteView, NotificationPriority.High)
        voipNotification.show()
        CallAnalytics.addAnalytics(
            event = EventName.INCOMING_CALL_SHOWN,
            agoraCallId = callContext?.channelData?.getCallingId().toString(),
            agoraMentorId = callContext?.channelData?.getAgoraUid().toString()
        )
        soundManager.playSound()
        scope.launch {
            try{
                delay(20000)
                voipNotification.removeNotification()
                updateIncomingCallState(false)
                stopAudio()
                CallAnalytics.addAnalytics(
                    event = EventName.INCOMING_CALL_IGNORE,
                    agoraCallId = callContext?.channelData?.getCallingId().toString(),
                    agoraMentorId = callContext?.channelData?.getAgoraUid().toString()
                )
            }
            catch (e : Exception){
                if(e is CancellationException)
                    throw e
                e.printStackTrace()
            }

        }
    }

    private fun updateIncomingCallState(isShowingIncomingCall: Boolean) {
        scope.launch {
            try{
                incomingCallMutex.withLock {
                    this@CallingMediator.isShowingIncomingCall = isShowingIncomingCall
                }
            }
            catch (e : Exception){
                if(e is CancellationException)
                    throw e
                e.printStackTrace()
            }
        }
    }

    private fun isMessageForSameChannel(channel: String) = callContext?.hasChannelData() == true && channel == callContext?.channelData?.getChannel()


    private fun stopAudio() {
        try {
            soundManager.stopSound()
        } catch (e: Exception) {
            e.printStackTrace()
            if(e is CancellationException)
                throw e
        }
    }

    // TODO: Change Name
    fun disconnectCallFromWebrtc() {
        webrtcService.disconnectCall()
    }

    fun changeSpeaker() {}

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