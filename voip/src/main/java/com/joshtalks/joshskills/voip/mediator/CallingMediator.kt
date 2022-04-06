package com.joshtalks.joshskills.voip.mediator

import android.media.RingtoneManager
import com.joshtalks.joshskills.base.constants.INCOMING
import com.joshtalks.joshskills.base.constants.PEER_TO_PEER
import com.joshtalks.joshskills.voip.audiomanager.SOUND_TYPE_RINGTONE
import com.joshtalks.joshskills.voip.audiomanager.SoundManager
import com.joshtalks.joshskills.voip.calldetails.CallDetails
import com.joshtalks.joshskills.voip.calldetails.IncomingCallData
import com.joshtalks.joshskills.voip.communication.EventChannel
import com.joshtalks.joshskills.voip.communication.PubNubChannelService
import com.joshtalks.joshskills.voip.communication.constants.ServerConstants
import com.joshtalks.joshskills.voip.communication.model.ChannelData
import com.joshtalks.joshskills.voip.communication.model.Error
import com.joshtalks.joshskills.voip.communication.model.IncomingCall
import com.joshtalks.joshskills.voip.communication.model.MessageData
import com.joshtalks.joshskills.voip.communication.model.OutgoingData
import com.joshtalks.joshskills.voip.communication.model.PeerToPeerCallRequest
import com.joshtalks.joshskills.voip.communication.model.UserAction
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
import com.joshtalks.joshskills.voip.mediator.CallDirection.*
import com.joshtalks.joshskills.voip.notification.NotificationPriority
import com.joshtalks.joshskills.voip.notification.VoipNotification
import com.joshtalks.joshskills.voip.pstn.PSTNInterface
import com.joshtalks.joshskills.voip.pstn.PSTNListener
import com.joshtalks.joshskills.voip.pstn.PSTNState
import com.joshtalks.joshskills.voip.voipLog
import com.joshtalks.joshskills.voip.webrtc.AgoraCallingService
import com.joshtalks.joshskills.voip.webrtc.CallState
import com.joshtalks.joshskills.voip.webrtc.CallingService
import kotlin.Exception
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.*
import kotlin.collections.HashMap
import kotlinx.coroutines.delay

class CallingMediator(val scope: CoroutineScope) : CallServiceMediator {
    private val callingService: CallingService by lazy { AgoraCallingService }
    private val networkEventChannel: EventChannel by lazy { PubNubChannelService }
    private val pstnService: PSTNInterface = PSTNListener()
    private lateinit var callDirection: CallDirection
    private var calling = PeerToPeerCalling()
    private var callType = 0
    private val flow by lazy { MutableSharedFlow<Int>(replay = 0) }
    private val mutex = Mutex(false)
    private val soundManager by lazy { SoundManager(SOUND_TYPE_RINGTONE,20000) }
    private lateinit var voipNotification : VoipNotification

    init {
        scope.launch {
            mutex.withLock {
                observerPstnService()
                callingService.initCallingService()
                networkEventChannel.initChannel()
                handleWebrtcEvent()
                handlePubnubEvent()
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
                    calling.onPreCallConnect(callData)
                } catch (e: Exception) {
                    flow.emit(ERROR)
                    voipLog?.log("Connect Call API Failed")
                    e.printStackTrace()
                }
            }
        }
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

    override fun switchAudio() {}

    override fun disconnectCall() {
        voipLog?.log("Disconnect Call")
        callingService.disconnectCall()
    }

    override fun observeState(): SharedFlow<Int> {
        return callingService.observeCallingState()
    }

    private fun observerPstnService() {
        voipLog?.log("Listining PSTN")
        scope.launch {
            pstnService.observePSTNState().collect {
                when (it) {
                    PSTNState.Idle -> {
                        voipLog?.log("IDEL")
                        val data =
                            UserAction(type = ServerConstants.RESUME, callId = CallDetails.callId)
                        networkEventChannel.emitEvent(data)
                        flow.emit(UNHOLD)
                    }
                    PSTNState.OnCall, PSTNState.Ringing -> {
                        voipLog?.log("ON CALL")
                        val data = UserAction(
                            type = ServerConstants.ONHOLD,
                            callId = CallDetails.callId
                        )
                        networkEventChannel.emitEvent(data)
                        flow.emit(HOLD)
                    }
                }
            }
        }
    }

    // Handle Events coming from Backend
    private fun handlePubnubEvent() {
        scope.launch {
            networkEventChannel.observeChannelEvents().collectLatest {
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
        voipNotification = VoipNotification(remoteView,NotificationPriority.High)
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