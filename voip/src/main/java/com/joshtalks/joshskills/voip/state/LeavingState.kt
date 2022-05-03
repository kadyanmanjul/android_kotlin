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

    private val scope = CoroutineScope(Dispatchers.IO)

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
        scope.launch {
            try {
                ensureActive()
                val event = context.getStreamPipe().receive()
                ensureActive()
                if (event.what == CALL_DISCONNECTED) {
                    Log.d(TAG, "observe: Joined Channel --> ${context.channelData.getChannel()}")
                    context.updateUIState(uiState = UIState.empty())
                    ensureActive()
                    PrefManager.setVoipState(State.IDLE)
                    context.destroyContext()
                } else
                    throw IllegalEventException("In $TAG but received ${event.what} expected $CALL_INITIATED_EVENT")
                // TODO: Handle Error Case
                scope.cancel()
            } catch (e: Exception) {
                e.printStackTrace()
                PrefManager.setVoipState(State.IDLE)
                context.destroyContext()
            }
        }
    }
}