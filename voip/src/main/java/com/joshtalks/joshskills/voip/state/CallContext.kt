package com.joshtalks.joshskills.voip.state

import android.os.SystemClock
import android.util.Log
import com.joshtalks.joshskills.voip.communication.model.ChannelData
import com.joshtalks.joshskills.voip.communication.model.OutgoingData
import com.joshtalks.joshskills.voip.constant.Event
import com.joshtalks.joshskills.voip.data.UIState
import com.joshtalks.joshskills.voip.data.local.PrefManager
import com.joshtalks.joshskills.voip.mediator.CallDirection
import com.joshtalks.joshskills.voip.mediator.CallingMediator
import com.joshtalks.joshskills.voip.voipanalytics.CallAnalytics
import com.joshtalks.joshskills.voip.voipanalytics.EventName
import com.joshtalks.joshskills.voip.webrtc.Envelope
import com.joshtalks.joshskills.voip.webrtc.RECONNECTING_TIMEOUT_IN_MILLIS
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel

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

    fun hasChannelData() = this::channelData.isInitialized


    fun connect() {
        Log.d(TAG, "connect")

        state.connect()
    }

    fun disconnect() {
        Log.d(TAG, "disconnect")
        state.disconnect()
    }

    fun backPress() {
        Log.d(TAG, "Back pressed")
        state.backPress()
    }

    fun disconnectCall() {
        Log.d(TAG, "disconnect Call From Webrtc ")
        mediator.disconnectCallFromWebrtc()
    }

    fun updateUIState(uiState: UIState) {
        Log.d(TAG, "Updating UI state $uiState ")
        currentUiState = uiState
        mediator.uiStateFlow.value = currentUiState
    }

    fun joinChannel(channelData: ChannelData) {
        Log.d(TAG, "Join Channel with $channelData")
        mediator.joinChannel(channelData)
    }

    fun destroyContext() {
        Log.d(TAG, "destroyContext: ")
        closePipe()
        destroyState()
        // Change Current State to Idle
    }

    fun destroyState() {
        Log.d(TAG, "Destroy State ")
        state.onDestroy()
    }

    fun onError() {
        Log.d(TAG, "OnError ")
        state.onError()
    }

    fun closePipe() {
        Log.d(TAG, "closePipe: Closing")
        mediator.stateChannel.close()
        Log.d(TAG, "closePipe: Closed")
    }

    fun reconnecting() {
        Log.d(TAG, "reconnecting Before Delay ")
        reconnectingJob = scope.launch {
            try{
                delay(RECONNECTING_TIMEOUT_IN_MILLIS)
                Log.d(TAG, "reconnecting After Delay ")
                CallAnalytics.addAnalytics(
                    event = EventName.DISCONNECTED_BY_RECONNECTING,
                    agoraCallId =channelData.getCallingId().toString(),
                    agoraMentorId = channelData.getAgoraUid().toString()
                )
                state.disconnect()
            }
            catch (e : Exception){
                if(e is CancellationException)
                    throw e
                e.printStackTrace()
            }
        }
    }

    fun reconnected() {
        Log.d(TAG, "Reconnected After Reconnecting")
        reconnectingJob.cancel()
    }

    suspend fun closeCallScreen() {
        Log.d(TAG, "closeCallScreen ")
        val envelope = Envelope(Event.CLOSE_CALL_SCREEN)
        sendEventToUI(envelope)
    }

    fun getStreamPipe()  : Channel<Envelope<Event>> {
        Log.d(TAG, "Get Stream Pipe ${mediator.stateChannel} ")
        return mediator.stateChannel
    }

    fun sendMessageToServer(event : OutgoingData) {
        Log.d(TAG, "sendMessageToServer $event} ")
        mediator.sendEventToServer(event)
    }

    suspend fun sendEventToUI(event : Envelope<Event>) {
        Log.d(TAG, "sendEventToUI $event} ")
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
        Log.d(TAG, "sendMessageToServer $isMuted} ")
        mediator.muteAudio(isMuted)
    }
}

class UnexpectedException(message: String) : Throwable(message = message)
class IllegalEventException(message: String) : Throwable(message = message)