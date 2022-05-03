package com.joshtalks.joshskills.voip.state

import android.os.Message
import android.os.SystemClock
import android.util.Log
import com.joshtalks.joshskills.voip.communication.constants.ServerConstants
import com.joshtalks.joshskills.voip.communication.model.Channel
import com.joshtalks.joshskills.voip.communication.model.ChannelData
import com.joshtalks.joshskills.voip.communication.model.NetworkAction
import com.joshtalks.joshskills.voip.communication.model.OutgoingData
import com.joshtalks.joshskills.voip.constant.CLOSE_CALL_SCREEN
import com.joshtalks.joshskills.voip.data.UIState
import com.joshtalks.joshskills.voip.inSeconds
import com.joshtalks.joshskills.voip.mediator.CallDirection
import com.joshtalks.joshskills.voip.mediator.CallingMediator
import com.joshtalks.joshskills.voip.webrtc.RECONNECTING_TIMEOUT_IN_MILLIS
import kotlinx.coroutines.*
import java.util.concurrent.TimeUnit

data class CallContext(val callType: Int, val direction : CallDirection, val request: HashMap<String, Any>, private val mediator: CallingMediator)  {
    private val TAG = "CallContext"

    var state : VoipState = IdleState(this)
    lateinit var channelData : ChannelData
    var currentUiState = UIState.empty()
    private set
    val scope = CoroutineScope(Dispatchers.IO)
    private lateinit var reconnectingJob : Job
    val durationInMillis by lazy {
        callDurationInMillis()
    }


    fun connect() {
        state.connect()
    }

    fun disconnect() {
        state.disconnect()
    }

    fun backPress() {
        state.backPress()
    }

    fun disconnectCall() {
        mediator.disconnectCallFromWebrtc()
    }

    fun updateUIState(uiState: UIState) {
        currentUiState = uiState
        mediator.uiStateFlow.value = currentUiState
    }

    fun joinChannel(channelData: ChannelData) {
        mediator.joinChannel(channelData)
    }

    fun destroyContext() {
        closePipe()
        destroyState()
        // Change Current State to Idle
    }

    fun destroyState() {
        state.onDestroy()
    }

    fun onError() {
        state.onError()
    }

    fun closePipe() {
        mediator.stateChannel.close()
    }

    fun reconnecting() {
        reconnectingJob = scope.launch {
            delay(RECONNECTING_TIMEOUT_IN_MILLIS)
            state.disconnect()
        }
    }

    fun reconnected() {
        reconnectingJob.cancel()
    }

    suspend fun closeCallScreen() {
        val msg = Message.obtain().apply {
            what = CLOSE_CALL_SCREEN
        }
        sendEventToUI(msg)
    }

    fun getStreamPipe()  : kotlinx.coroutines.channels.Channel<Message> {
        return mediator.stateChannel
    }

    fun sendMessageToServer(event : OutgoingData) {
        mediator.sendEventToServer(event)
    }

    suspend fun sendEventToUI(event : Message) {
        mediator.flow.emit(event)
    }

    private fun callDurationInMillis(): Long {
        val startTime = currentUiState.startTime
        if(startTime == 0L)
            return 0L
        val currentTime = SystemClock.elapsedRealtime()
        Log.d(TAG, "callDurationInMillis: Started at - $startTime and End at - $currentTime")
        return currentTime - startTime
    }

    fun changeMicState(isMuted : Boolean) {
        mediator.muteAudio(isMuted)
    }
}

class UnexpectedException(message: String) : Throwable(message = message)
class IllegalEventException(message: String) : Throwable(message = message)