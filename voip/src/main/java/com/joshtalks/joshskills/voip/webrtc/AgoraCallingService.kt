package com.joshtalks.joshskills.voip.webrtc

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

object AgoraCallingService : CallingService {
    // TODO: Need to change name
    private enum class State {
        IDLE,
        JOINING,
        JOINED,
        CONNECTED,
        LEAVING
    }

    @Volatile
    private var agoraEngine: RtcEngine? = null
    private val eventFlow : MutableSharedFlow<CallState> = MutableSharedFlow(replay = 0)
    private val scope = CoroutineScope(Dispatchers.IO)
    private var state = State.IDLE
    private val agoraEvent by lazy {
        AgoraEventHandler.getAgoraEventObject(scope)
    }
    private var callRequest : CallRequest? = null

    init { observeCallbacks() }

    override suspend fun initCallingService() {
        withContext(scope.coroutineContext) {
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

        scope.launch {
            voipLog?.log("Connecting Call $agoraEngine")
            initCallingService()
            val status = joinChannel(request)
            voipLog?.log("Join Channel Status ----> ${status}")
            state = State.JOINING
            // TODO: Need to check status if its unable to join
            // 1. API Call to notify backend Start Listening to Pubnub Channel
            // 2. Will send timeout (Use a Timer/repeat/loop and break-out from it when receive channel through pubnub)
            // 3. Receive Token and Channel through Pubnub [Pubnub Module]
            // 4. Join Channel through Agora SDK

        }
    }

    private fun joinChannel(request : CallRequest) : Int? {
        voipLog?.log("Joining Channel")
        return agoraEngine?.joinChannel(
            request.getToken(),
            request.getChannel(),
            "ENTER_OPTIONAL_INFO",
            0
        )
    }

    private fun leaveChannel() {
        voipLog?.log("Leaving Channel")
        agoraEngine?.leaveChannel()
    }

    override fun disconnectCall() {
        voipLog?.log("Disconnecting Call")
        scope.launch {
            // 1. Send DISCONNECTING signal through Pubnub
            // 2. Leave Channel through Agora SDK
            voipLog?.log("Coroutine : About to call leaveChannel")
            leaveChannel()
            state = State.LEAVING
            voipLog?.log("Coroutine : Finishing call leaveChannel Coroutine")
        }
    }

    private fun observeCallbacks() {
        scope.launch {
            agoraEvent.callingEvent.collect { callState ->
                    voipLog?.log("observeCallbacks : CallState = $callState")
                    when(callState) {
                        CallState.CallDisconnected, CallState.Idle -> state = State.IDLE
                        CallState.CallConnected -> state = State.CONNECTED
                        CallState.CallInitiated -> state = State.JOINED
                        else -> {}
                    }
                voipLog?.log("observeCallbacks : CallState = $callState")
                eventFlow.emit(callState)
            }
        }
    }

    override fun observeCallingEvents(): Flow<CallState> {
        voipLog?.log("Setting event")
        return eventFlow
    }

    fun showToast(msg : String) {
        //Log.d(TAG, "showToast : $msg")
    }
}