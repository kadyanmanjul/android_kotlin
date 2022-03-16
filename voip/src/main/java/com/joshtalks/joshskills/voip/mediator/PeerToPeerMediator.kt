package com.joshtalks.joshskills.voip.mediator

import com.joshtalks.joshskills.voip.communication.EventChannel
import com.joshtalks.joshskills.voip.communication.PubNubChannelService
import com.joshtalks.joshskills.voip.communication.constants.DISCONNECTED
import com.joshtalks.joshskills.voip.communication.constants.ONHOLD
import com.joshtalks.joshskills.voip.communication.model.ChannelData
import com.joshtalks.joshskills.voip.communication.model.Error
import com.joshtalks.joshskills.voip.communication.model.IncomingCall
import com.joshtalks.joshskills.voip.communication.model.MessageData
import com.joshtalks.joshskills.voip.communication.model.PeerToPeerCallRequest
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

class PeerToPeerMediator(private val observerFlow : SharedFlow<Int>) : CallServiceMediator {
    private val callingService : CallingService by lazy { AgoraCallingService }
    private val networkEventChannel : EventChannel by lazy { PubNubChannelService }
    private val scope = CoroutineScope(Dispatchers.IO)
    private val flow by lazy {
        MutableSharedFlow<Int>(replay = 0)
    }

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

    override fun connectCall() {
        //callingService.connectCall()
    }

    override fun switchAudio() {
        TODO("Not yet implemented")
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
                        callingService.disconnectCall()
                        flow.emit(it.errorType)
                    }
                    is ChannelData -> {
                        /**
                         * Join Channel
                         */
                        AgoraCallingService.showToast("Channel Data ${it}")
                        val request = PeerToPeerCallRequest(
                            channelName = it.getChannel(),
                            callToken = it.getCallingToken()
                        )
                        callingService.connectCall(request)
                    }
                    is MessageData -> {
                        when(it.getType()) {
                            ONHOLD -> {
                                // Transfer to Service
                            }
                            DISCONNECTED -> {
                                callingService.disconnectCall()
                            }
                        }
                    }
                    is IncomingCall -> {
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
                        voipLog?.log("Call Connected")
                    }
                    CallState.CallDisconnected -> {
                        voipLog?.log("Call Disconnected")
                    }
                    CallState.CallInitiated -> {
                        // CallInitiated
                        voipLog?.log("Call CallInitiated")
                    }
                    CallState.OnReconnected -> {

                    }
                    CallState.OnReconnecting -> {

                    }
                }
            }
        }
    }
}