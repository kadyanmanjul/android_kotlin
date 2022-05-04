package com.joshtalks.joshskills.voip.state

import android.util.Log
import com.joshtalks.joshskills.voip.constant.Event.*
import com.joshtalks.joshskills.voip.constant.State
import com.joshtalks.joshskills.voip.data.UIState
import com.joshtalks.joshskills.voip.data.local.PrefManager
import kotlinx.coroutines.*

// Fired Leave Channel and waiting for Leave Channel Callback
class LeavingState(val context: CallContext) : VoipState {
    private val TAG = "LeavingState"
    private val scope = CoroutineScope(Dispatchers.IO + CoroutineExceptionHandler {
            coroutineContext, throwable ->
        Log.d(TAG, "CoroutineExceptionHandler : $throwable")
        throwable.printStackTrace()
    })

    init {
        Log.d("Call State", TAG)
        observe()
    }

    override fun onError() {
        context.destroyContext()
    }

    override fun onDestroy() {
        scope.cancel()
    }

    // Handle Events related to Connected State
    private fun observe() {
        Log.d(TAG, "Started Observing")
        scope.launch {
            try {
                ensureActive()
                val event = context.getStreamPipe().receive()
                Log.d(TAG, "Received after observing : ${event.type}")
                ensureActive()
                if (event.type == CALL_DISCONNECTED) {
                    context.closePipe()
                    context.updateUIState(uiState = UIState.empty())
                    ensureActive()
                    PrefManager.setVoipState(State.IDLE)
                    Log.d(TAG, "OBSERVE : ${event.type} switched to IDLE STATE")
                    context.destroyContext()
                } else {
                    ensureActive()
                    throw IllegalEventException("In $TAG but received ${event.type} expected $CALL_DISCONNECTED")
                }
                scope.cancel()
            } catch (e: Throwable) {
                if(e is CancellationException)
                    throw e
                if(e is IllegalEventException)
                    e.printStackTrace()
                e.printStackTrace()
                PrefManager.setVoipState(State.IDLE)
                Log.d(TAG, "EXCEPTION : $e switched to IDLE STATE")
                context.destroyContext()
            }
        }
    }
}