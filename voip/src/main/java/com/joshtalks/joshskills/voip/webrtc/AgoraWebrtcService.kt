package com.joshtalks.joshskills.voip.webrtc

import android.content.Context.AUDIO_SERVICE
import android.media.AudioManager
import android.util.Log
import com.joshtalks.joshskills.voip.BuildConfig
import com.joshtalks.joshskills.voip.Utils
import com.joshtalks.joshskills.voip.constant.CONNECTED
import com.joshtalks.joshskills.voip.constant.IDLE
import com.joshtalks.joshskills.voip.constant.JOINED
import com.joshtalks.joshskills.voip.constant.JOINING
import com.joshtalks.joshskills.voip.constant.LEAVING
import com.joshtalks.joshskills.voip.constant.LEAVING_AND_JOINING
import com.joshtalks.joshskills.voip.data.local.PrefManager
import com.joshtalks.joshskills.voip.voipLog
import io.agora.rtc.RtcEngine
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.collect
import timber.log.Timber

private const val JOINING_CHANNEL_SUCCESS = 0
private const val USER_ALREADY_IN_A_CHANNEL = -17
private const val TAG = "AgoraWebrtcService"

internal class AgoraWebrtcService(val scope: CoroutineScope) : WebrtcService {
    private var reconnectingJob : Job? = null
    private var agoraEngine: RtcEngine? = null
    private val eventFlow : MutableSharedFlow<CallState> = MutableSharedFlow(replay = 0)
    private var state = MutableSharedFlow<Int>(replay = 0)
    // TODO: Make State More reliable
    private var currentState = IDLE

    private lateinit var lazyJoin : Deferred<Unit>
    private val listener by lazy { AgoraEventHandler(scope) }

    init {
        Log.d(TAG, " INIT : ${agoraEngine}")
        agoraEngine = RtcEngine.create(
            Utils.context,
            BuildConfig.AGORA_API_KEY,
            listener
        ).apply {
            setParameters("{\"rtc.peer.offline_period\":5000}")
            setParameters("{\"che.audio.keep.audiosession\":true}")
        }
        Log.d(TAG, "initWebrtcService: ${agoraEngine}")
        observeCallbacks()
    }

    override fun connectCall(request: CallRequest) {
        scope.launch {
            voipLog?.log("Connecting Call $agoraEngine")
            state.emit(JOINING)
            currentState = JOINING
            val status = joinChannel(request)
            voipLog?.log("Join Channel Status ----> $status")
            when(status) {
                JOINING_CHANNEL_SUCCESS -> {}
                USER_ALREADY_IN_A_CHANNEL -> {
                    state.emit(LEAVING_AND_JOINING)
                    currentState = LEAVING_AND_JOINING
                    createLazyJoinRequest(request)
                    leaveChannel()
                }
                else -> {
                    state.emit(IDLE)
                    currentState = IDLE
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
        scope.launch {
            // 1. Send DISCONNECTING signal through Pubnub
            // 2. Leave Channel through Agora SDK
            stopReconnectingTimeoutTimer()
            stopLazyJoin()
            voipLog?.log("Coroutine : About to call leaveChannel")
            state.emit(LEAVING)
            currentState = LEAVING
            leaveChannel()
            voipLog?.log("Coroutine : Finishing call leaveChannel Coroutine")
        }
    }

    private fun createLazyJoinRequest(request: CallRequest) {
        scope.launch {
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
        if(this@AgoraWebrtcService::lazyJoin.isInitialized)
            lazyJoin.cancel()
    }

    private fun startLazyJoin() {
        if(this@AgoraWebrtcService::lazyJoin.isInitialized)
            lazyJoin.start()
    }

    override fun muteAudioStream(muteAudio : Boolean) {
        agoraEngine?.muteLocalAudioStream(muteAudio)
    }

    override fun enableSpeaker(speaker: Boolean) {
        agoraEngine?.setEnableSpeakerphone(speaker)
    }

    override fun observeCallingEvents(): SharedFlow<CallState> {
        voipLog?.log("Setting event")
        return eventFlow
    }

    override fun observeCallingState(): SharedFlow<Int> {
        voipLog?.log("Setting event")
        return state
    }

    override fun onDestroy() {
        agoraEngine?.removeHandler(listener)
        RtcEngine.destroy()
        agoraEngine = null
    }

    private fun joinChannel(request : CallRequest) : Int? {
        voipLog?.log("Joining Channel")
        agoraEngine?.apply {
            val audio = Utils.context?.getSystemService(AUDIO_SERVICE) as AudioManager
            val maxVolume = audio.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL)
            val currentVolume = audio.getStreamVolume(AudioManager.STREAM_VOICE_CALL)
            adjustPlaybackSignalVolume((95 / maxVolume) * currentVolume)
            enableDeepLearningDenoise(true)
        }
        return agoraEngine?.joinChannel(
            request.getToken(),
            request.getChannel(),
            "new_p2p_arch",
            request.getAgoraUId()
        )
    }

    private fun leaveChannel() {
        Log.d(TAG, "leaveChannel: ")
        agoraEngine?.leaveChannel()
    }

    private fun observeCallbacks() {
        scope.launch {
            listener.observeCallEvents().collect { callState ->
                voipLog?.log("observeCallbacks : CallState = $callState .... $state")
                when(callState) {
                    CallState.CallDisconnected, CallState.Idle -> {
                        if(currentState == LEAVING_AND_JOINING) {
                            voipLog?.log("LEAVING_AND_JOINING")
                            startLazyJoin()
                        }
                        else {
                            state.emit(IDLE)
                            currentState = IDLE
                        }
                        eventFlow.emit(callState)
                    }
                    CallState.CallConnected -> {
                        //TODO: Use Reconnecting
                        if(currentState == CONNECTED) {
                            stopReconnectingTimeoutTimer()
                            eventFlow.emit(CallState.OnReconnected)
                        } else {
                            state.emit(CONNECTED)
                            currentState = CONNECTED
                            eventFlow.emit(callState)
                        }
                    }
                    CallState.CallInitiated -> {
                        state.emit(JOINED)
                        currentState = JOINED
                        eventFlow.emit(callState)
                    }
                    CallState.OnReconnecting -> {
                        startReconnectingTimeoutTimer()
                        eventFlow.emit(callState)
                    }
                    CallState.OnReconnected -> {
                        stopReconnectingTimeoutTimer()
                        eventFlow.emit(callState)
                    }
                    CallState.UserAlreadyDisconnectedError -> {
                        state.emit(IDLE)
                        currentState = IDLE
                        eventFlow.emit(callState)
                    }
                    CallState.Error -> {
                        if(currentState == JOINED || currentState == CONNECTED || currentState == JOINING) {
                            Log.d("disconnectCall()", "observeCallbacks: ")
                            disconnectCall()
                        }
                        else {
                            state.emit(IDLE)
                            currentState = IDLE
                        }
                        eventFlow.emit(callState)
                    }
                }
                voipLog?.log("observeCallbacks : CallState = $callState")
            }
        }
    }

    private fun startReconnectingTimeoutTimer() {
        if(reconnectingJob?.isActive != true) {
            reconnectingJob = scope.launch {
                delay(RECONNECTING_TIMEOUT_IN_MILLIS)
                eventFlow.emit(CallState.ReconnectingFailed)
                Log.d("disconnectCall()", " Disconnecting due to Reconnecting")
                disconnectCall()
            }
        }
    }

    fun stopReconnectingTimeoutTimer() {
        Log.d(TAG, "Cancelling Reconnect Timer")
        reconnectingJob?.cancel()
    }
}