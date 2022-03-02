package com.joshtalks.joshskills.ui.call.lib

import com.joshtalks.joshskills.BuildConfig
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.ui.call.lib.AgoraCallingService.State.*
import io.agora.rtc.RtcEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val USER_ALREADY_JOINED = -17

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
    private val eventFlow : MutableStateFlow<CallState> = MutableStateFlow(CallState.Idle)
    private val scope = CoroutineScope(Dispatchers.IO)
    private var state = IDLE
    private val agoraEvent by lazy {
        AgoraEventHandler.getAgoraEventObject(scope)
    }
    private var callRequest : CallRequest? = null

    init { observeCallbacks() }

    suspend fun initCallingService() {
        withContext(scope.coroutineContext) {
            if (agoraEngine == null)
                synchronized(this) {
                    if (agoraEngine != null)
                        agoraEngine
                    else
                        agoraEngine = RtcEngine.create(
                            AppObjectController.joshApplication,
                            BuildConfig.AGORA_API_KEY,
                            agoraEvent.handler
                        )
                }
        }
    }

    override fun connectCall(request: CallRequest) {
        scope.launch {
            initCallingService()
            val status = joinChannel()
            state = State.JOINING
            // TODO: Need to check status if its unable to join
            // 1. API Call to notify backend Start Listening to Pubnub Channel
            // 2. Will send timeout (Use a Timer/repeat/loop and break-out from it when receive channel through pubnub)
            // 3. Receive Token and Channel through Pubnub [Pubnub Module]
            // 4. Join Channel through Agora SDK

        }
    }

    private fun joinChannel() : Int? = agoraEngine?.joinChannel("ENTER_TOKEN", "ENTER_CHANNEL_NAME","ENTER_OPTIONAL_INFO",0)

    private fun leaveChannel() {
        agoraEngine?.leaveChannel()
    }

    override fun disconnectCall() {
        scope.launch {
            // 1. Send DISCONNECTING signal through Pubnub
            // 2. Leave Channel through Agora SDK
            leaveChannel()
            state = LEAVING
        }
    }

    private fun observeCallbacks() {
        scope.launch {
            agoraEvent.callingEvent.collect { callState ->
                    when(callState) {
                        CallState.CallDisconnected -> state = IDLE
                        CallState.CallConnected -> state = CONNECTED
                        CallState.CallInitiated -> state = JOINED
                        CallState.Idle -> state = IDLE
                    }
                eventFlow.emit(callState)
                eventFlow.collectLatest {  }
            }
        }
    }

    override fun observeCallingEvents(): Flow<CallState> = eventFlow
}
