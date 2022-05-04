package com.joshtalks.joshskills.voip.state

import android.util.Log
import com.joshtalks.joshskills.voip.constant.CALL_DISCONNECTED
import com.joshtalks.joshskills.voip.constant.CALL_INITIATED_EVENT
import com.joshtalks.joshskills.voip.constant.IDLE
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
        Log.d(TAG, "observe: ")
        scope.launch {
            try {
                ensureActive()
                val event = context.getStreamPipe().receive()
                ensureActive()
                if (event.what == CALL_DISCONNECTED) {
                    context.closePipe()
                    context.updateUIState(uiState = UIState.empty())
                    ensureActive()
                    PrefManager.setVoipState(State.IDLE)
                    context.destroyContext()
                } else {
                    ensureActive()
                    throw IllegalEventException("In $TAG but received ${event.what} expected $CALL_DISCONNECTED")
                }
                scope.cancel()
            } catch (e: Throwable) {
                if(e is CancellationException)
                    throw e
                if(e is IllegalEventException)
                    e.printStackTrace()
                e.printStackTrace()
                PrefManager.setVoipState(State.IDLE)
                context.destroyContext()
            }
        }
    }
}