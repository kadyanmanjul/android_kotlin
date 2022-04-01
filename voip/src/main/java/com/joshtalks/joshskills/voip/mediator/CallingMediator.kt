package com.joshtalks.joshskills.voip.mediator

import com.joshtalks.joshskills.voip.calldetails.CallDetails
import com.joshtalks.joshskills.voip.communication.EventChannel
import com.joshtalks.joshskills.voip.communication.PubNubChannelService
import com.joshtalks.joshskills.voip.communication.constants.MessageConstants
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
import com.joshtalks.joshskills.voip.constant.IDLE
import com.joshtalks.joshskills.voip.constant.JOINING
import com.joshtalks.joshskills.voip.constant.RECONNECTED
import com.joshtalks.joshskills.voip.constant.RECONNECTING
import com.joshtalks.joshskills.voip.constant.UNHOLD
import com.joshtalks.joshskills.voip.mediator.CallDirection.*
import com.joshtalks.joshskills.voip.pstn.PSTNInterface
import com.joshtalks.joshskills.voip.pstn.PSTNListener
import com.joshtalks.joshskills.voip.pstn.PSTNState
import com.joshtalks.joshskills.voip.voipLog
import com.joshtalks.joshskills.voip.webrtc.AgoraCallingService
import com.joshtalks.joshskills.voip.webrtc.CallState
import com.joshtalks.joshskills.voip.webrtc.CallingService
import java.lang.Exception
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class CallingMediator(val scope: CoroutineScope) : CallServiceMediator {
    private val callingService: CallingService by lazy { AgoraCallingService }
    private val networkEventChannel: EventChannel by lazy { PubNubChannelService }
    // AudioRouter
    //
    private val pstnService: PSTNInterface = PSTNListener()
    private lateinit var callDirection: CallDirection
    private var calling = PeerToPeerCalling()
    private val flow by lazy { MutableSharedFlow<Int>(replay = 0) }

    init {
        scope.launch {
            observerPstnService()
            callingService.initCallingService()
            networkEventChannel.initChannel()
            handleWebrtcEvent()
            handlePubnubEvent()
        }
    }

    override fun observeEvents(): SharedFlow<Int> {
        return flow
    }

    override fun connectCall(callType: CallType, callData: HashMap<String, Any>) {
        scope.launch {
            voipLog?.log("CallData Before Mutex --> $callData")
            try {
                setCallType(callType)
                // TODO: Need to Handle when Error Occurred
                voipLog?.log("Coroutine CallData --> $callData")
                calling.onPreCallConnect(callData)
            } catch (e: Exception) {
                voipLog?.log("Connect Call API Failed")
                e.printStackTrace()
            }
        }
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
                            UserAction(type = MessageConstants.RESUME, callId = CallDetails.callId)
                        networkEventChannel.emitEvent(data)
                        flow.emit(UNHOLD)
                    }
                    PSTNState.OnCall, PSTNState.Ringing -> {
                        voipLog?.log("ON CALL")
                        val data = UserAction(
                            type = MessageConstants.ONHOLD,
                            callId = CallDetails.callId
                        )
                        networkEventChannel.emitEvent(data)
                        flow.emit(HOLD)
                    }
                }
            }
        }
    }

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
                        CallDetails.set(it)
                    }
                    is MessageData -> {
                        voipLog?.log("Message Data -> $it")
                        when (it.getType()) {
                            MessageConstants.ONHOLD -> {
                                // Transfer to Service
                                flow.emit(HOLD)
                            }

                            MessageConstants.RESUME -> {
                                flow.emit(UNHOLD)
                            }

                            MessageConstants.DISCONNECTED -> {
                                callingService.disconnectCall()
                                flow.emit(CALL_DISCONNECT_REQUEST)
                            }
                        }
                    }
                    is IncomingCall -> {
                        voipLog?.log("Incoming Call -> $it")
                        updateCallDirection(CallDirection.INCOMING)
                        calling.notificationLayout()
                        /**
                         * Show Incoming Call Notification
                         */
                    }
                }
            }
        }
    }

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

    private fun setCallType(callType: CallType) {
        when (callType) {
            CallType.PEER_TO_PEER -> {}
            CallType.FPP -> {}
            CallType.GROUP -> {}
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