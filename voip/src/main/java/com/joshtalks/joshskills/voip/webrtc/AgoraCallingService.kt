package com.joshtalks.joshskills.voip.webrtc

import android.telecom.Call
import com.joshtalks.joshskills.voip.BuildConfig
import com.joshtalks.joshskills.voip.Utils
import com.joshtalks.joshskills.voip.voipLog
import io.agora.rtc.RtcEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
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
    private var state = MutableSharedFlow<State>(replay = 0)
    private val agoraEvent by lazy {
        AgoraEventHandler.getAgoraEventObject(ioScope)
    }
    private var callRequest : CallRequest? = null

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
            val status = joinChannel(request)
            if(status == JOINING_CHANNEL_SUCCESS)
                state.emit(State.JOINING)
            else {
                state.emit(State.IDLE)
                eventFlow.emit(CallState.Error)
            }
            voipLog?.log("Join Channel Status ----> ${status}")
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
            leaveChannel()
            state.emit(State.LEAVING)
            voipLog?.log("Coroutine : Finishing call leaveChannel Coroutine")
        }
    }

    override fun observeCallingEvents(): Flow<CallState> {
        voipLog?.log("Setting event")
        return eventFlow
    }

    override fun observeCallingState(): Flow<State> {
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
                    CallState.CallDisconnected, CallState.Idle -> state.emit(State.IDLE)
                    CallState.CallConnected -> state.emit(State.CONNECTED)
                    CallState.CallInitiated -> state.emit(State.JOINED)
                    CallState.Error -> {
                        if(state.equals(State.JOINED) || state.equals(State.CONNECTED) || state.equals(State.JOINING))
                            disconnectCall()
                        else
                            state.emit(State.IDLE)
                    }
                }
                voipLog?.log("observeCallbacks : CallState = $callState")
                eventFlow.emit(callState)
            }
        }
    }
}