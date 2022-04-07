package com.joshtalks.joshskills.voip.webrtc

import com.joshtalks.joshskills.voip.BuildConfig
import com.joshtalks.joshskills.voip.Utils
import com.joshtalks.joshskills.voip.constant.CONNECTED
import com.joshtalks.joshskills.voip.constant.IDLE
import com.joshtalks.joshskills.voip.constant.JOINED
import com.joshtalks.joshskills.voip.constant.JOINING
import com.joshtalks.joshskills.voip.constant.LEAVING
import com.joshtalks.joshskills.voip.constant.LEAVING_AND_JOINING
import com.joshtalks.joshskills.voip.voipLog
import io.agora.rtc.RtcEngine
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

private const val JOINING_CHANNEL_SUCCESS = 0
private const val USER_ALREADY_IN_A_CHANNEL = -17

internal object AgoraCallingService : CallingService {
    // TODO: Need to change name

    @Volatile
    private var agoraEngine: RtcEngine? = null
    private val eventFlow : MutableSharedFlow<CallState> = MutableSharedFlow(replay = 0)
    private val coroutineExceptionHandler = CoroutineExceptionHandler{_, e ->
        Timber.tag("Coroutine Exception").d("Handled...")
        e.printStackTrace()
    }
    private val ioScope = CoroutineScope(Dispatchers.IO + coroutineExceptionHandler)
    private var state = MutableSharedFlow<Int>(replay = 0)
    private lateinit var lazyJoin : Deferred<Unit>
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
            voipLog?.log("Join Channel Status ----> $status")
            when(status) {
                JOINING_CHANNEL_SUCCESS -> {}
                USER_ALREADY_IN_A_CHANNEL -> {
                    state.emit(LEAVING_AND_JOINING)
                    createLazyJoinRequest(request)
                    leaveChannel()
                }
                else -> {
                    state.emit(IDLE)
                    eventFlow.emit(CallState.Error)
                }
            }
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
            stopLazyJoin()
            voipLog?.log("Coroutine : About to call leaveChannel")
            state.emit(LEAVING)
            leaveChannel()
            voipLog?.log("Coroutine : Finishing call leaveChannel Coroutine")
        }
    }

    private fun createLazyJoinRequest(request: CallRequest) {
        ioScope.launch {
            lazyJoin = async(start = CoroutineStart.LAZY) {
                if(isActive) {
                    val status = joinChannel(request)
                    if (status != JOINING_CHANNEL_SUCCESS && isActive)
                        eventFlow.emit(CallState.Error)
                }
            }
        }
    }

    private fun stopLazyJoin() {
        if(this@AgoraCallingService::lazyJoin.isInitialized)
            lazyJoin.cancel()
    }

    private fun startLazyJoin() {
        if(this@AgoraCallingService::lazyJoin.isInitialized)
            lazyJoin.start()
    }

    override fun muteAudioStream(muteAudio : Boolean) {
        agoraEngine?.muteLocalAudioStream(muteAudio)
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
                    CallState.CallDisconnected, CallState.Idle -> {
                        if(state.equals(LEAVING_AND_JOINING))
                            startLazyJoin()
                        else
                            state.emit(IDLE)
                    }
                    CallState.CallConnected -> state.emit(CONNECTED)
                    CallState.CallInitiated -> state.emit(JOINED)
                    CallState.ReconnectingFailed -> { disconnectCall() }
                    CallState.UserAlreadyDisconnectedError -> {
                        state.emit(IDLE)
                    }
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