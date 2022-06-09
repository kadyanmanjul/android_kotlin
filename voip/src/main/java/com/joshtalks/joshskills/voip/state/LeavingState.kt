package com.joshtalks.joshskills.voip.state

import android.util.Log
import com.joshtalks.joshskills.voip.constant.Event.*
import com.joshtalks.joshskills.voip.constant.State
import com.joshtalks.joshskills.voip.data.UIState
import com.joshtalks.joshskills.voip.data.local.PrefManager
import com.joshtalks.joshskills.voip.voipanalytics.CallAnalytics
import com.joshtalks.joshskills.voip.voipanalytics.EventName
import kotlinx.coroutines.*

// Fired Leave Channel and waiting for Leave Channel Callback
class LeavingState(val context: CallContext) : VoipState {
    private val TAG = "LeavingState"
    private val scope =
        CoroutineScope(Dispatchers.IO + CoroutineExceptionHandler { coroutineContext, throwable ->
            Log.d(TAG, "CoroutineExceptionHandler : $throwable")
            throwable.printStackTrace()
        })

    init {
        Log.d("Call State", TAG)
        CallAnalytics.addAnalytics(
            event = EventName.CHANNEL_LEAVING,
            agoraCallId = context.channelData.getCallingId().toString(),
            agoraMentorId = context.channelData.getAgoraUid().toString()
        )
        observe()
    }

    override fun onError(reason: String) {
        CallAnalytics.addAnalytics(
            event = EventName.ON_ERROR,
            agoraCallId = context.channelData.getCallingId().toString(),
            agoraMentorId = context.channelData.getAgoraUid().toString(),
            extra = "In $TAG : $reason"
        )
        scope.launch { context.closeCallScreen() }
        context.closePipe()
        onDestroy()
    }

    override fun onDestroy() {
        scope.cancel()
    }

    // Handle Events related to Connected State
    private fun observe() {
        Log.d(TAG, "Started Observing")
        scope.launch {
            loop@ while (true) {
                try {
                    ensureActive()
                    val event = context.getStreamPipe().receive()
                    Log.d(TAG, "Received after observing : ${event.type}")
                    ensureActive()
                    if (event.type == CALL_DISCONNECTED) {
                        CallAnalytics.addAnalytics(
                            event = EventName.CHANNEL_LEFT,
                            agoraCallId = context.channelData.getCallingId().toString(),
                            agoraMentorId = context.channelData.getAgoraUid().toString()
                        )
                        context.closePipe()
                        context.updateUIState(uiState = UIState.empty())
                        ensureActive()
                        PrefManager.setVoipState(State.IDLE)
                        Log.d(TAG, "OBSERVE : ${event.type} switched to IDLE STATE")
                        context.closeCallScreen()
                        onDestroy()
                        break@loop
                    } else {
                        ensureActive()
                        val msg = "In $TAG but received ${event.type} expected $CALL_DISCONNECTED"
                        CallAnalytics.addAnalytics(
                            event = EventName.ILLEGAL_EVENT_RECEIVED,
                            agoraCallId = context.channelData.getCallingId().toString(),
                            agoraMentorId = context.channelData.getAgoraUid().toString(),
                            extra = msg
                        )
                        throw IllegalEventException(msg)
                    }
                } catch (e: Throwable) {
                    if (e is CancellationException)
                        throw e
                    if (e is IllegalEventException) {
                        e.printStackTrace()
                    } else {
                        e.printStackTrace()
                        PrefManager.setVoipState(State.IDLE)
                        Log.d(TAG, "EXCEPTION : $e switched to IDLE STATE")
                        context.closeCallScreen()
                        context.closePipe()
                        onDestroy()
                    }
                }
            }
        }
    }
}