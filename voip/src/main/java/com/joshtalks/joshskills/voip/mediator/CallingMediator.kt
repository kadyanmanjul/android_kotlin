package com.joshtalks.joshskills.voip.mediator

import android.util.Log
import com.joshtalks.joshskills.base.constants.INTENT_DATA_INCOMING_CALL_ID
import com.joshtalks.joshskills.voip.communication.fallback.FirebaseChannelService
import com.joshtalks.joshskills.voip.calldetails.IncomingCallData
import com.joshtalks.joshskills.voip.communication.EventChannel
import com.joshtalks.joshskills.voip.communication.PubNubChannelService
import com.joshtalks.joshskills.voip.communication.PubnubState.CONNECTED
import com.joshtalks.joshskills.voip.communication.PubnubState.DISCONNECTED
import com.joshtalks.joshskills.voip.communication.PubnubState.RECONNECTED
import com.joshtalks.joshskills.voip.communication.constants.ServerConstants
import com.joshtalks.joshskills.voip.communication.model.*
import com.joshtalks.joshskills.voip.constant.*
import com.joshtalks.joshskills.voip.data.ServiceEvents
import com.joshtalks.joshskills.voip.data.UIState
import com.joshtalks.joshskills.voip.data.local.PrefManager
import com.joshtalks.joshskills.voip.notification.IncomingCallNotificationHandler
import com.joshtalks.joshskills.voip.state.CallContext
import com.joshtalks.joshskills.voip.voipanalytics.CallAnalytics
import com.joshtalks.joshskills.voip.voipanalytics.EventName
import com.joshtalks.joshskills.voip.webrtc.AgoraWebrtcService
import com.joshtalks.joshskills.voip.webrtc.CallState
import com.joshtalks.joshskills.voip.webrtc.Envelope
import com.joshtalks.joshskills.voip.webrtc.WebrtcService
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

const val PER_USER_TIMEOUT_IN_MILLIS = 10 * 1000L
const val FAV_USER_TIMEOUT_IN_MILLIS = 50 * 1000L
private const val TAG = "CallingMediator"
enum class ActionDirection {
    SERVER,
    LOCAL
}

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

    var calling : CallCategory = PeerToPeerCall()
    private set
    private var callCategory : Category = Category.PEER_TO_PEER

    val flow by lazy {
        MutableSharedFlow<Envelope<Event>>(replay = 0)
    }
    val uiStateFlow = MutableStateFlow(UIState.empty())
    val uiTransitionFlow = MutableSharedFlow<ServiceEvents>(replay = 0)
    private val mutex = Mutex(false)
    private val incomingNotificationMutex = Mutex(false)
    private lateinit var incomingCallNotificationHandler: IncomingCallNotificationHandler
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

    override fun connectCall(callCategory: Category, callData: HashMap<String, Any>) {
        Log.d(TAG, "connectCall: ${scope.isActive} ")
        // TODO: IMPORTANT- We have to make sure that in any case this should not be called twice
        scope.launch {
            Log.d(TAG, "connectCall : Inside Scope")
            try{
                mutex.withLock {
                    calling = when(callCategory) {
                        Category.PEER_TO_PEER -> PeerToPeerCall()
                        Category.FPP -> FavoriteCall()
                        Category.GROUP -> GroupCall()
                    }
                    PrefManager.setCallCategory(callCategory)
                    /**
                     * Using State Pattern
                     */
                    Log.d(TAG, "connectCall : Inside Lock")
                    if (this@CallingMediator::incomingCallNotificationHandler.isInitialized) {
                        incomingCallNotificationHandler.removeNotification()
                    }
                    callContext?.destroyContext()
                    stateChannel = Channel(Channel.UNLIMITED)
                    callContext = CallContext(
                        callType = callCategory,
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


    override fun hideIncomingCall() {
        scope.launch {
            try {
                val map = HashMap<String, Any>(1).apply {
                    put(INTENT_DATA_INCOMING_CALL_ID, IncomingCallData.callId)
                }
                calling.onCallDecline(map)
                incomingCallNotificationHandler.removeNotification()
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
                        if(callContext?.hasChannelData() == true) {
                            CallAnalytics.addAnalytics(
                                event = EventName.PSTN_CALL_RECEIVED,
                                agoraCallId = callContext?.channelData?.getCallingId().toString(),
                                agoraMentorId = callContext?.channelData?.getAgoraUid().toString()
                            )
                        }
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
                    UserAction.TOPIC_IMAGE_CHANGE -> {
                        val envelope = Envelope(Event.TOPIC_IMAGE_CHANGE_REQUEST)
                        stateChannel.send(envelope)
                    }
                    UserAction.START_RECORDING -> {
                        val envelope = Envelope(Event.START_RECORDING, data = ActionDirection.SERVER)
                        stateChannel.send(envelope)
                    }
                    UserAction.STOP_RECORDING -> {
                        val envelope = Envelope(Event.STOP_RECORDING, data = ActionDirection.SERVER)
                        stateChannel.send(envelope)
                    }
                    UserAction.RECORDING_REQUEST_ACCEPTED -> {
                        val envelope = Envelope(Event.CALL_RECORDING_ACCEPT, data = ActionDirection.SERVER)
                        stateChannel.send(envelope)
                    }
                    UserAction.RECORDING_REQUEST_REJECTED -> {
                        val envelope = Envelope(Event.CALL_RECORDING_REJECT, data = ActionDirection.SERVER)
                        stateChannel.send(envelope)
                    }
                    UserAction.CANCEL_RECORDING_REQUEST -> {
                        val envelope = Envelope(Event.CANCEL_RECORDING_REQUEST, data = ActionDirection.SERVER)
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
                    Log.d(TAG, "observeChannelState : $it")
                    try{
                        when (it) {
                            CONNECTED ->  {}
                            RECONNECTED -> {
                                val envelope = Envelope(Event.SYNC_UI_STATE)
                                stateChannel.send(envelope)
                            }
                            DISCONNECTED -> {}
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
        if (this@CallingMediator::incomingCallNotificationHandler.isInitialized) {
            incomingCallNotificationHandler.removeNotification()
        }
        networkEventChannel.onDestroy()
        try {
            fallbackEventChannel.onDestroy()
        } catch (e : Exception) {
            e.printStackTrace()
        }
        webrtcService.onDestroy()
    }

    override fun stopAgoraCallRecording() {
        stopRecording()
    }

    override fun startAgoraCallRecording() {
        startRecording()
    }

    private fun handleWebrtcEvent() {
        scope.launch {
            try {
                webrtcService.observeCallingEvents().collect {
                    try{
                        Log.d(TAG, "handleWebrtcEvent : $it")
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
                                CallAnalytics.addAnalytics(
                                    event = EventName.CALL_RECONNECTING,
                                    agoraCallId = PrefManager.getAgraCallId().toString(),
                                    agoraMentorId = PrefManager.getLocalUserAgoraId().toString()
                                )
                                val envelope = Envelope(Event.RECONNECTING)
                                stateChannel.send(envelope)
                            }
                            is CallState.Error -> {
                                callContext?.onError(it.reason)
                            }
                            CallState.UserLeftChannel -> {
                                val envelope = Envelope(Event.REMOTE_USER_DISCONNECTED_USER_LEFT)
                                stateChannel.send(envelope)
                            }
                            CallState.RecordingGenerated -> {
                                val envelope = Envelope(Event.AGORA_CALL_RECORDED)
                                flow.emit(envelope)
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
                            processNetworkEvent(event)
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
                        processNetworkEvent(it)
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

    private suspend fun processNetworkEvent(event : Communication) {
        when (event) {
            is Error -> {
                callContext?.onError(event.reason)
            }
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
                        ServerConstants.TOPIC_IMAGE_RECEIVED ->{
                            val envelope = Envelope(
                                Event.TOPIC_IMAGE_RECEIVED,
                                event.getMsgData()
                            )
                            stateChannel.send(envelope)
                        }
                        // Remote User Disconnected
                        ServerConstants.DISCONNECTED -> {
                            val envelope = Envelope(Event.REMOTE_USER_DISCONNECTED_MESSAGE)
                            stateChannel.send(envelope)
                        }
                        ServerConstants.START_RECORDING -> {
                            val envelope = Envelope(Event.START_RECORDING, data = ActionDirection.LOCAL)
                            stateChannel.send(envelope)
                        }
                        ServerConstants.STOP_RECORDING -> {
                            val envelope = Envelope(Event.STOP_RECORDING, data = ActionDirection.LOCAL)
                            stateChannel.send(envelope)
                        }
                        ServerConstants.CALL_RECORDING_ACCEPT -> {
                            val envelope = Envelope(Event.CALL_RECORDING_ACCEPT, data = ActionDirection.LOCAL)
                            stateChannel.send(envelope)
                        }
                        ServerConstants.CALL_RECORDING_REJECT -> {
                            val envelope = Envelope(Event.CALL_RECORDING_REJECT, data = ActionDirection.LOCAL)
                            stateChannel.send(envelope)
                        }
                        ServerConstants.CANCEL_RECORDING_REQUEST -> {
                            val envelope = Envelope(Event.CANCEL_RECORDING_REQUEST, data = ActionDirection.LOCAL)
                            stateChannel.send(envelope)
                        }
                    }
                }
            }
            is IncomingCall -> {
                val map = HashMap<String,String>()
                map[INCOMING_CALL_CATEGORY] = Category.PEER_TO_PEER.category
                map[INCOMING_CALL_ID] = event.getCallId().toString()
                handleIncomingCall(map)
            }
            is GroupIncomingCall -> {
                val map = HashMap<String,String>()
                map[INCOMING_CALL_CATEGORY] = Category.GROUP.category
                map[INCOMING_CALL_ID] = event.getCallId().toString()
                handleIncomingCall(map)
            }
            is UI -> {
                if (isMessageForSameChannel(event.getChannelName())) {
                    val envelope = Envelope(Event.UI_STATE_UPDATED,event)
                    stateChannel.send(envelope)
                }
            }
        }
    }

    override suspend fun handleIncomingCall(map: HashMap<String, String>) {
        val callType = map[INCOMING_CALL_ID]
        incomingNotificationMutex.withLock {
            val ifNotificationVisible = if(this@CallingMediator::incomingCallNotificationHandler.isInitialized){
                incomingCallNotificationHandler.isNotificationVisible()
            }else{
                false
            }
            if (ifNotificationVisible.not() && PrefManager.getVoipState() == State.IDLE) {
                when (callType) {
                    Category.PEER_TO_PEER.category -> {
                        callCategory = Category.PEER_TO_PEER
                        calling =PeerToPeerCall()
                    }
                    Category.FPP.category -> {
                        callCategory = Category.FPP
                        calling =PeerToPeerCall()
                    }
                    Category.GROUP.category-> {
                        callCategory = Category.GROUP
                        calling = GroupCall()
                    }
                }
                PrefManager.setCallCategory(callCategory)
                PrefManager.setIncomingCallId(map[INCOMING_CALL_ID]!!.toInt())
                CallAnalytics.addAnalytics(
                    event = EventName.INCOMING_CALL_RECEIVED,
                    agoraCallId = map[INCOMING_CALL_ID],
                    agoraMentorId = "-1"
                )
                incomingCallNotificationHandler = IncomingCallNotificationHandler()
                incomingCallNotificationHandler.inflateNotification(map)
            }
        }
    }

    private fun isMessageForSameChannel(channel: String) = callContext?.hasChannelData() == true && channel == callContext?.channelData?.getChannel()

    // TODO: Change Name
    suspend fun disconnectCallFromWebrtc() {
        webrtcService.disconnectCall()
    }

    fun changeSpeaker(isEnable:Boolean) {
        webrtcService.enableSpeaker(isEnable)
    }

    fun startRecording() {
        webrtcService.onStartRecording()
    }

    fun stopRecording() {
        webrtcService.onStopRecording()
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

    suspend fun joinChannel(channel: ChannelData) {
        val request = PeerToPeerCallRequest(
            channelName = channel.getChannel(),
            callToken = channel.getCallingToken(),
            agoraUId = channel.getAgoraUid()
        )
        webrtcService.connectCall(request)
    }
}