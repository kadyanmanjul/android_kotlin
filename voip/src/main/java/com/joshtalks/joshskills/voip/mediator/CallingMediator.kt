package com.joshtalks.joshskills.voip.mediator

import com.joshtalks.joshskills.voip.communication.EventChannel
import com.joshtalks.joshskills.voip.communication.PubNubChannelService
import com.joshtalks.joshskills.voip.communication.constants.MessageConstants
import com.joshtalks.joshskills.voip.communication.model.ChannelData
import com.joshtalks.joshskills.voip.communication.model.Error
import com.joshtalks.joshskills.voip.communication.model.IncomingCall
import com.joshtalks.joshskills.voip.communication.model.MessageData
import com.joshtalks.joshskills.voip.communication.model.PeerToPeerCallRequest
import com.joshtalks.joshskills.voip.constant.CALL_CONNECTED
import com.joshtalks.joshskills.voip.constant.CALL_DISCONNECTED
import com.joshtalks.joshskills.voip.constant.CALL_INITIATED
import com.joshtalks.joshskills.voip.constant.HOLD
import com.joshtalks.joshskills.voip.constant.MUTE
import com.joshtalks.joshskills.voip.constant.RECONNECTED
import com.joshtalks.joshskills.voip.constant.RECONNECTING
import com.joshtalks.joshskills.voip.constant.UNHOLD
import com.joshtalks.joshskills.voip.mediator.CallDirection.*
import com.joshtalks.joshskills.voip.voipLog
import com.joshtalks.joshskills.voip.webrtc.AgoraCallingService
import com.joshtalks.joshskills.voip.webrtc.CallState
import com.joshtalks.joshskills.voip.webrtc.CallingService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class CallingMediator(private val observerFlow : SharedFlow<Int>) : CallServiceMediator {
    private val callingService : CallingService by lazy { AgoraCallingService }
    private val networkEventChannel : EventChannel by lazy { PubNubChannelService }
    // AudioRouter
    // Call Details
    //
    private lateinit var callDirection : CallDirection
    private var calling = PeerToPeerCalling()
    private val scope = CoroutineScope(Dispatchers.IO)
    private val flow by lazy { MutableSharedFlow<Int>(replay = 0) }

    init {
        scope.launch {
            observerFlow.collect {

            }
        }
        scope.launch {
            callingService.initCallingService()
            handleWebrtcEvent()
        }
        scope.launch {
            networkEventChannel.initChannel()
            handlePubnubEvent()
        }
    }

    override fun observeEvents(): SharedFlow<Int> {
        return flow
    }

    override fun connectCall(callType: CallType) {
        setCallType(callType)
        calling.onPreCallConnect()

    }

    override fun switchAudio() {

    }

    override fun disconnectCall() {
        callingService.disconnectCall()
    }

    private fun handlePubnubEvent() {
        scope.launch {
            networkEventChannel.observeChannelEvents().collectLatest {
                when(it) {
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
                    }
                    is MessageData -> {
                        voipLog?.log("Message Data -> $it")
                        when(it.getType()) {
                            MessageConstants.ONHOLD -> {
                                // Transfer to Service
                                flow.emit(HOLD)
                            }

                            MessageConstants.RESUME -> {
                                flow.emit(UNHOLD)
                            }

                            MessageConstants.DISCONNECTED -> {
                                callingService.disconnectCall()
                            }
                        }
                    }
                    is IncomingCall -> {
                        voipLog?.log("Incoming Call -> $it")
                        updateCallDirection(INCOMING)
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
                when(it) {
                    CallState.CallConnected -> {
                        // Call Connected
                        flow.emit(CALL_CONNECTED)
                        voipLog?.log("Call Connected")
                    }
                    CallState.CallDisconnected -> {
                        flow.emit(CALL_DISCONNECTED)
                        voipLog?.log("Call Disconnected")
                    }
                    CallState.CallInitiated -> {
                        // CallInitiated
                        flow.emit(CALL_INITIATED)
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
                }
            }
        }
    }

    private fun setCallType(callType: CallType) {
        when(callType) {
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