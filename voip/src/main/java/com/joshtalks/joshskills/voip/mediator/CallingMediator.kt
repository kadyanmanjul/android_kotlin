package com.joshtalks.joshskills.voip.mediator

import android.media.RingtoneManager
import android.util.Log
import com.joshtalks.joshskills.base.constants.INCOMING
import com.joshtalks.joshskills.base.constants.INTENT_DATA_INCOMING_CALL_ID
import com.joshtalks.joshskills.base.constants.PEER_TO_PEER
import com.joshtalks.joshskills.voip.FirebaseChannelService
import com.joshtalks.joshskills.voip.audiomanager.SOUND_TYPE_RINGTONE
import com.joshtalks.joshskills.voip.audiomanager.SoundManager
import com.joshtalks.joshskills.voip.calldetails.CallDetails
import com.joshtalks.joshskills.voip.calldetails.IncomingCallData
import com.joshtalks.joshskills.voip.communication.EventChannel
import com.joshtalks.joshskills.voip.communication.PubNubChannelService
import com.joshtalks.joshskills.voip.communication.constants.ServerConstants
import com.joshtalks.joshskills.voip.communication.model.*
import com.joshtalks.joshskills.voip.constant.CALL_CONNECTED_EVENT
import com.joshtalks.joshskills.voip.constant.CALL_DISCONNECTED
import com.joshtalks.joshskills.voip.constant.CALL_DISCONNECT_REQUEST
import com.joshtalks.joshskills.voip.constant.CALL_INITIATED_EVENT
import com.joshtalks.joshskills.voip.constant.ERROR
import com.joshtalks.joshskills.voip.constant.HOLD
import com.joshtalks.joshskills.voip.constant.INCOMING_CALL
import com.joshtalks.joshskills.voip.constant.MUTE
import com.joshtalks.joshskills.voip.constant.RECONNECTED
import com.joshtalks.joshskills.voip.constant.RECONNECTING
import com.joshtalks.joshskills.voip.constant.UNHOLD
import com.joshtalks.joshskills.voip.constant.UNMUTE
import com.joshtalks.joshskills.voip.data.local.PrefManager
import com.joshtalks.joshskills.voip.mediator.CallDirection.*
import com.joshtalks.joshskills.voip.notification.NotificationPriority
import com.joshtalks.joshskills.voip.notification.VoipNotification
import com.joshtalks.joshskills.voip.voipLog
import com.joshtalks.joshskills.voip.webrtc.AgoraCallingService
import com.joshtalks.joshskills.voip.webrtc.CallState
import com.joshtalks.joshskills.voip.webrtc.CallingService
import kotlin.Exception
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.*
import kotlin.collections.HashMap
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

const val PER_USER_TIMEOUT_IN_MILLIS = 10 * 1000L
const val CALL_CONNECT_TIMEOUT_IN_MILLIS = 2 * 60 * 1000L
private const val TAG = "CallingMediator"

class CallingMediator(val scope: CoroutineScope) : CallServiceMediator {
    private val callingService: CallingService by lazy { AgoraCallingService }
    private val networkEventChannel: EventChannel by lazy { PubNubChannelService }
    private val fallbackEventChannel: EventChannel by lazy { FirebaseChannelService }
    private lateinit var callDirection: CallDirection
    private var calling = PeerToPeerCalling()
    private var callType = 0
    private val flow by lazy { MutableSharedFlow<Int>(replay = 0) }
    private val mutex = Mutex(false)
    private val soundManager by lazy { SoundManager(SOUND_TYPE_RINGTONE,20000) }
    private lateinit var voipNotification : VoipNotification
    private lateinit var userNotFoundJob : Job
    private var latestEventTimestamp = PrefManager.getLatestPubnubMessageTime()
    private val Communication?.hasMainEventChannelFailed : Boolean
        get() {
            return latestEventTimestamp <= (this?.getEventTime() ?: 0)
        }

    init {
        scope.launch {
            mutex.withLock {
                callingService.initCallingService()
                networkEventChannel.initChannel()
                fallbackEventChannel.initChannel()
                handleWebrtcEvent()
                handlePubnubEvent()
                handleFallbackEvents()
            }
        }
    }

    override fun observeEvents(): SharedFlow<Int> {
        return flow
    }

    private fun stopAudio() {
        try {
            soundManager.stopSound()
        } catch (e : Exception) {
            e.printStackTrace()
        }
    }

    override fun connectCall(callType: Int, callData: HashMap<String, Any>) {
        scope.launch {
            mutex.withLock {
                voipLog?.log("CallData Before Mutex --> $callData")
                try {
                    // Saving this to set Calltype when we receive Channel details
                    this@CallingMediator.callType = callType
                    // TODO: Need to Handle when Error Occurred
                    voipLog?.log("Coroutine CallData --> $callData")
                    if(this@CallingMediator::voipNotification.isInitialized) {
                        voipNotification.removeNotification()
                        stopAudio()
                    }

                    // TODO: Need to Fix Checking Incoming call two time
                    if(callData.isIncomingCall()) {
                        stopUserNotFoundTimer()
                    } else {
                        stopUserNotFoundTimer()
                        startUserNotFoundTimer()
                    }
                    calling.onPreCallConnect(callData)
                } catch (e: Exception) {
                    flow.emit(ERROR)
                    voipLog?.log("Connect Call API Failed")
                    e.printStackTrace()
                }
            }
        }
    }

    private fun HashMap<String, Any>.isIncomingCall() : Boolean {
        return get(INTENT_DATA_INCOMING_CALL_ID) != null
    }

    private fun handleFallbackEvents() {
        Log.d(TAG, "handleFallbackEvents: Pubnub Channel Failed...")
        scope.launch {
            fallbackEventChannel.observeChannelEvents().collectLatest { event ->
                if(event.hasMainEventChannelFailed) {
                    networkEventChannel.reconnect()
                    when (event) {
                        is Error -> {
                            /**
                             * Reset Webrtc
                             */
                            voipLog?.log("Error --> $event")
                            // TODO: Should not handle this here
                            callingService.disconnectCall()
                            flow.emit(event.errorType)
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
                            callingService.connectCall(request)
                            CallDetails.reset()
                            CallDetails.set(event, callType)
                        }
                        is MessageData -> {
                            voipLog?.log("Message Data -> $event")
                            Log.d(TAG, "handleFallbackEvents: $event")
                            if (event.isMessageForSameChannel()) {
                                when (event.getType()) {
                                    ServerConstants.ONHOLD -> {
                                        // Transfer to Service
                                        flow.emit(HOLD)
                                    }
                                    ServerConstants.RESUME -> {
                                        flow.emit(UNHOLD)
                                    }
                                    ServerConstants.MUTE -> {
                                        flow.emit(MUTE)
                                    }
                                    ServerConstants.UNMUTE -> {
                                        flow.emit(UNMUTE)
                                    }
                                    ServerConstants.DISCONNECTED -> {
                                        callingService.disconnectCall()
                                        flow.emit(CALL_DISCONNECT_REQUEST)
                                    }
                                }
                            }
                        }
                        is IncomingCall -> {
                            voipLog?.log("Incoming Call -> $event")
                            IncomingCallData.set(event.getCallId(), PEER_TO_PEER)
                            flow.emit(INCOMING_CALL)
                        }
                    }
                }
            }
        }
    }
    
    private fun startUserNotFoundTimer() {
        userNotFoundJob = scope.launch {
            if(isActive) {
                val timeout = Timeout(ServerConstants.TIMEOUT)
                for(i in 1..12) {
                    Log.d(TAG, "startUserNotFoundTimer: ")
                    delay(PER_USER_TIMEOUT_IN_MILLIS)
                    if(isActive)
                        networkEventChannel.emitEvent(timeout)
                    else
                        break
                }
                val networkAction = NetworkAction(
                    callId = -1,
                    uid = -1,
                    type = ServerConstants.DISCONNECTED,
                    duration = 0
                )
                networkEventChannel.emitEvent(networkAction)
                disconnectCall()
                flow.emit(CALL_DISCONNECT_REQUEST)
            }
        }
    }

    private fun stopUserNotFoundTimer() {
        Log.d(TAG, "stopUserNotFoundTimer: ")
        if(::userNotFoundJob.isInitialized)
            userNotFoundJob.cancel()
    }

    override fun muteAudioStream(muteAudio: Boolean) {
        callingService.muteAudioStream(muteAudio)
    }

    override fun sendEventToServer(data: OutgoingData) {
        networkEventChannel.emitEvent(data)
    }

    override fun showIncomingCall(incomingCall : IncomingCall) {
        showIncomingNotification(incomingCall)
    }

    override fun hideIncomingCall() {
        scope.launch {
            val map = HashMap<String, Any>(1).apply {
                put(INTENT_DATA_INCOMING_CALL_ID, IncomingCallData.callId)
            }
            calling.onCallDecline(map)
            stopAudio()
            voipNotification.removeNotification()
        }
    }

    override fun switchAudio() {}

    override fun disconnectCall() {
        voipLog?.log("Disconnect Call")
        stopUserNotFoundTimer()
        callingService.disconnectCall()
    }

    override fun observeState(): SharedFlow<Int> {
        return callingService.observeCallingState()
    }

    // Handle Events coming from Backend
    private fun handlePubnubEvent() {
        scope.launch {
            networkEventChannel.observeChannelEvents().collectLatest {
                latestEventTimestamp = it.getEventTime() ?: 0L
                PrefManager.setLatestPubnubMessageTime(latestEventTimestamp)
                when (it) {
                    is Error -> {
                        /**
                         * Reset Webrtc
                         */
                        voipLog?.log("Error --> $it")
                        // TODO: Should not handle this here
                        callingService.disconnectCall()
                        flow.emit(it.errorType)
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
                        callingService.connectCall(request)
                        CallDetails.reset()
                        CallDetails.set(it, callType)
                    }
                    is MessageData -> {
                        voipLog?.log("Message Data -> $it")
                        if (it.isMessageForSameChannel()) {
                            when (it.getType()) {
                                ServerConstants.ONHOLD -> {
                                    // Transfer to Service
                                    flow.emit(HOLD)
                                }
                                ServerConstants.RESUME -> {
                                    flow.emit(UNHOLD)
                                }
                                ServerConstants.MUTE -> {
                                    flow.emit(MUTE)
                                }
                                ServerConstants.UNMUTE -> {
                                    flow.emit(UNMUTE)
                                }
                                ServerConstants.DISCONNECTED -> {
                                    callingService.disconnectCall()
                                    flow.emit(CALL_DISCONNECT_REQUEST)
                                }
                            }
                        }
                    }
                    is IncomingCall -> {
                        voipLog?.log("Incoming Call -> $it")
                        IncomingCallData.set(it.getCallId(), PEER_TO_PEER)
                        flow.emit(INCOMING_CALL)
                    }
                }
            }
        }
    }

    private fun showIncomingNotification(incomingCall : IncomingCall) {
        val remoteView = calling.notificationLayout(incomingCall) ?: return // TODO: might throw error
        voipNotification = VoipNotification(remoteView , NotificationPriority.High)
        voipNotification.show()
        soundManager.playSound()
        scope.launch {
            delay(20000)
            voipNotification.removeNotification()
            stopAudio()
        }
    }

    private fun MessageData.isMessageForSameChannel() =
        this.getChannel() == CallDetails.agoraChannelName

    private fun handleWebrtcEvent() {
        scope.launch {
            callingService.observeCallingEvents().collectLatest {
                when (it) {
                    CallState.CallConnected -> {
                        // Call Connected
                        flow.emit(CALL_CONNECTED_EVENT)
                        voipLog?.log("Call Connected")
                    }
                    CallState.CallDisconnected -> {
                        flow.emit(CALL_DISCONNECTED)
                        voipLog?.log("Call Disconnected")
                    }
                    CallState.ReconnectingFailed -> {
                        flow.emit(CALL_DISCONNECT_REQUEST)
                        voipLog?.log("Call Disconnect Request")
                    }
                    CallState.CallInitiated -> {
                        // CallInitiated
                        flow.emit(CALL_INITIATED_EVENT)
                        voipLog?.log("Call CallInitiated")
                    }
                    CallState.OnReconnected -> {
                        flow.emit(RECONNECTED)
                        voipLog?.log("OnReconnected")
                    }

                    CallState.OnReconnecting -> {
                        flow.emit(RECONNECTING)
                        voipLog?.log("OnReconnecting")
                    }
                    CallState.Error -> {
                        flow.emit(ERROR)
                        voipLog?.log("Error")
                    }
                }
            }
        }
    }

    private fun updateCallDirection(direction: CallDirection) {
        callDirection = direction
    }
}

enum class CallDirection {
    INCOMING,
    OUTGOING
}