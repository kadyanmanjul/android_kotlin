package com.joshtalks.joshskills.voip.webrtc

import android.telecom.Call
import com.joshtalks.joshskills.voip.BuildConfig
import com.joshtalks.joshskills.voip.Utils
import com.joshtalks.joshskills.voip.constant.CONNECTED
import com.joshtalks.joshskills.voip.constant.IDLE
import com.joshtalks.joshskills.voip.constant.JOINED
import com.joshtalks.joshskills.voip.constant.JOINING
import com.joshtalks.joshskills.voip.constant.LEAVING
import com.joshtalks.joshskills.voip.voipLog
import io.agora.rtc.RtcEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val JOINING_CHANNEL_SUCCESS = 0

internal object AgoraCallingService : CallingService {
    // TODO: Need to change name

    @Volatile
    private var agoraEngine: RtcEngine? = null
    private val eventFlow : MutableSharedFlow<CallState> = MutableSharedFlow(replay = 0)
    private val ioScope = CoroutineScope(Dispatchers.IO)
    private var state = MutableSharedFlow<Int>(replay = 0)
    private val agoraEvent by lazy {
        AgoraEventHandler.getAgoraEventObject(ioScope)
    }

    init { observeCallbacks() }

    override suspend fun initCallingService() {
        withContext(ioScope.coroutineContext) {
            if (agoraEngine == null)
                synchronized(this) {
                    if (agoraEngine != null)
                        agoraEngine
                    else
                        agoraEngine = RtcEngine.create(
                            Utils.context,
                            BuildConfig.AGORA_API_KEY,
                            agoraEvent.handler
                        )
                }
        }
    }

    override fun connectCall(request: CallRequest) {
        ioScope.launch {
            voipLog?.log("Connecting Call $agoraEngine")
            initCallingService()
            state.emit(JOINING)
            val status = joinChannel(request)
            if(status != JOINING_CHANNEL_SUCCESS) {
                state.emit(IDLE)
                eventFlow.emit(CallState.Error)
            }
            voipLog?.log("Join Channel Status ----> $status")
            // TODO: Need to check status if its unable to join
            // 1. API Call to notify backend Start Listening to Pubnub Channel
            // 2. Will send timeout (Use a Timer/repeat/loop and break-out from it when receive channel through pubnub)
            // 3. Receive Token and Channel through Pubnub [Pubnub Module]
            // 4. Join Channel through Agora SDK
        }
    }

    override fun disconnectCall() {
        voipLog?.log("Disconnecting Call")
        ioScope.launch {
            // 1. Send DISCONNECTING signal through Pubnub
            // 2. Leave Channel through Agora SDK
            voipLog?.log("Coroutine : About to call leaveChannel")
            state.emit(LEAVING)
            leaveChannel()
            voipLog?.log("Coroutine : Finishing call leaveChannel Coroutine")
        }
    }

    override fun observeCallingEvents(): SharedFlow<CallState> {
        voipLog?.log("Setting event")
        return eventFlow
    }

    override fun observeCallingState(): SharedFlow<Int> {
        voipLog?.log("Setting event")
        return state
    }

    private fun joinChannel(request : CallRequest) : Int? {
        voipLog?.log("Joining Channel")
        return agoraEngine?.joinChannel(
            request.getToken(),
            request.getChannel(),
            "new_p2p_arch",
            request.getAgoraUId()
        )
    }

    private fun leaveChannel() {
        voipLog?.log("Leaving Channel")
        agoraEngine?.leaveChannel()
    }

    private fun observeCallbacks() {
        ioScope.launch {
            agoraEvent.callingEvent.collect { callState ->
                voipLog?.log("observeCallbacks : CallState = $callState")
                when(callState) {
                    CallState.CallDisconnected, CallState.Idle -> state.emit(IDLE)
                    CallState.CallConnected -> state.emit(CONNECTED)
                    CallState.CallInitiated -> state.emit(JOINED)
                    CallState.Error -> {
                        if(state.equals(JOINED) || state.equals(CONNECTED) || state.equals(JOINING))
                            disconnectCall()
                        else
                            state.emit(IDLE)
                    }
                }
                voipLog?.log("observeCallbacks : CallState = $callState")
                eventFlow.emit(callState)
            }
        }
    }
}