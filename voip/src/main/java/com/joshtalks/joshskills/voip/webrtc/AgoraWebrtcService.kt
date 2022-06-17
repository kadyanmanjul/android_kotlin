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
import com.joshtalks.joshskills.voip.getTempFileForCallRecording
import io.agora.rtc.Constants
import io.agora.rtc.RtcEngine
import io.agora.rtc.audio.AudioRecordingConfiguration
import java.io.File
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch


private const val JOINING_CHANNEL_SUCCESS = 0
private const val USER_ALREADY_IN_A_CHANNEL = -17
private const val TAG = "AgoraWebrtcService"

internal class AgoraWebrtcService(val scope: CoroutineScope) : WebrtcService {
    private var agoraEngine: RtcEngine? = null
    private val eventFlow: MutableSharedFlow<CallState> = MutableSharedFlow(replay = 0)
    private var state = MutableSharedFlow<Int>(replay = 0)

    // TODO: Make State More reliable
    private var currentState = IDLE
    private var recordFile : File? = null

    private lateinit var lazyJoin: Deferred<Unit>
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

            //            TODO:SOUND PROBLEM
            setParameters("{\"che.audio.enable.aec\":false}")  //for automatic echo cancelling
            setParameters("{\"che.audio.enable.agc\":true}")  //automatic gain control
            setParameters("{\"che.audio.enable.ns\":false}")   //noise suppression
        }
        Log.d(TAG, "initWebrtcService: ${agoraEngine}")
        observeCallbacks()
    }

    override suspend fun connectCall(request: CallRequest) {
            try {
                Log.d(TAG, "Connect Call Request : $request")
                state.emit(JOINING)
                currentState = JOINING
                val status = joinChannel(request)
                Log.d(TAG, "Join Channel Status ----> $status")
                when (status) {
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
                        eventFlow.emit(CallState.Error("In AgoraWebrtcService Class, connectCall Method Received Error Joining Status : $status"))
                    }
                }
                // 1. API Call to notify backend Start Listening to Pubnub Channel
                // 2. Will send timeout (Use a Timer/repeat/loop and break-out from it when receive channel through pubnub)
                // 3. Receive Token and Channel through Pubnub [Pubnub Module]
                // 4. Join Channel through Agora SDK
            } catch (e: Exception) {
                if (e is CancellationException)
                    throw e
                e.printStackTrace()
            }
    }

    override suspend fun disconnectCall() {
        Log.d(TAG, "Disconnecting Call")
            try {
                // 1. Send DISCONNECTING signal through Pubnub
                // 2. Leave Channel through Agora SDK
                stopLazyJoin()
                state.emit(LEAVING)
                currentState = LEAVING
                leaveChannel()
            } catch (e: Exception) {
                if (e is CancellationException)
                    throw e
                e.printStackTrace()
            }
    }

    private fun createLazyJoinRequest(request: CallRequest) {
        scope.launch {
            try {
                lazyJoin = async(start = CoroutineStart.LAZY) {
                    if (isActive) {
                        val status = joinChannel(request)
                        if (status != JOINING_CHANNEL_SUCCESS && isActive)
                            eventFlow.emit(CallState.Error("Tried Joining new channel after leaving old one still got Error Join Status : $status"))
                    }
                }
            } catch (e: Exception) {
                if (e is CancellationException)
                    throw e
                e.printStackTrace()
            }
        }
    }

    private fun stopLazyJoin() {
        if (this@AgoraWebrtcService::lazyJoin.isInitialized)
            lazyJoin.cancel()
    }

    private fun startLazyJoin() {
        if (this@AgoraWebrtcService::lazyJoin.isInitialized)
            lazyJoin.start()
    }

    override fun muteAudioStream(muteAudio: Boolean) {
        agoraEngine?.muteLocalAudioStream(muteAudio)
    }

    override fun enableSpeaker(speaker: Boolean) {
        agoraEngine?.setEnableSpeakerphone(speaker)
    }

    override fun observeCallingEvents(): SharedFlow<CallState> {
        return eventFlow
    }

    override fun onDestroy() {
        agoraEngine?.removeHandler(listener)
        RtcEngine.destroy()
        agoraEngine = null
    }

    override fun onStartRecording() {
        if (recordFile!=null){
            return
        }
        Utils.context?.getTempFileForCallRecording()?.let { file->
            recordFile = file
            Log.d(TAG, "onStartRecording called with: file = $file")
            agoraEngine?.startAudioRecording(AudioRecordingConfiguration(file.absolutePath,3,0,48000))
        }
    }

    override fun onStopRecording() {
            recordFile?.absolutePath?.let {
                agoraEngine?.stopAudioRecording()
                PrefManager.saveLastRecordingPath(recordFile!!.absolutePath)
                Log.e(TAG, "onStartRecording: started recording$recordFile")
                scope.launch {
                    try {
                        eventFlow.emit(CallState.RecordingGenerated)
                    } catch (e : Exception) {
                        e.printStackTrace()
                    }
                }
                recordFile = null
            }
    }

    private fun joinChannel(request: CallRequest): Int? {
        Log.d(TAG, "joinChannel: $request")
        agoraEngine?.apply {
            val audio = Utils.context?.getSystemService(AUDIO_SERVICE) as AudioManager
            val maxVolume = audio.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL)
            val currentVolume = audio.getStreamVolume(AudioManager.STREAM_VOICE_CALL)
            adjustPlaybackSignalVolume((95 / maxVolume) * currentVolume)
            adjustRecordingSignalVolume(400)
            setAudioProfile(Constants.AUDIO_PROFILE_DEFAULT, Constants.AUDIO_SCENARIO_DEFAULT)
//            TODO:SOUND PROBLEM
            enableDeepLearningDenoise(false)
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
            try {
                listener.observeCallEvents().collect { callState ->
                    try {
                        Log.d(TAG, "observeCallbacks : CallState = $callState .... $state")
                        when (callState) {
                            CallState.CallDisconnected, CallState.Idle -> {
                                if (currentState == LEAVING_AND_JOINING) {
                                    Log.d(TAG, "observeCallbacks: LEAVING_AND_JOINING")
                                    startLazyJoin()
                                } else {
                                    state.emit(IDLE)
                                    currentState = IDLE
                                    eventFlow.emit(callState)
                                }
                            }
                            CallState.CallConnected -> {
                                //TODO: Use Reconnecting
                                if (currentState == CONNECTED) {
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
                                eventFlow.emit(callState)
                            }
                            CallState.OnReconnected -> {
                                eventFlow.emit(callState)
                            }
                            CallState.UserAlreadyDisconnectedError -> {
                                state.emit(IDLE)
                                currentState = IDLE
                                eventFlow.emit(callState)
                            }
                            is CallState.Error -> {
                                if (currentState == JOINED || currentState == CONNECTED || currentState == JOINING) {
                                    Log.d("disconnectCall()", "observeCallbacks: ")
                                    disconnectCall()
                                } else {
                                    state.emit(IDLE)
                                    currentState = IDLE
                                }
                                eventFlow.emit(callState)
                            }
                            CallState.UserLeftChannel -> {
                                eventFlow.emit(callState)
                            }
                        }
                    } catch (e: Exception) {
                        if (e is CancellationException)
                            throw e
                        e.printStackTrace()
                    }
                }
            } catch (e: Exception) {
                if (e is CancellationException)
                    throw e
                e.printStackTrace()
            }
        }
    }
}

// TODO: Will Use this class instead of Message
data class Envelope<T : Enum<T>>(val type: T, val data: Any? = null)