package com.joshtalks.joshskills.voip.state

import android.util.Log
import com.joshtalks.joshskills.voip.constant.IDLE
import com.joshtalks.joshskills.voip.constant.State
import com.joshtalks.joshskills.voip.data.local.PrefManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

// Can make Calls
class IdleState(val context: CallContext) : VoipState {
    private val TAG = "IdleState"
    val scope = CoroutineScope(Dispatchers.IO)

    init {
        Log.d("Call State", TAG)
    }

    override fun connect() {
        Log.d(TAG, "connect: $context")
        PrefManager.setVoipState(State.SEARCHING)
        context.state = SearchingState(context)
    }

    override fun onError() {
        scope.launch {
            context.closeCallScreen()
            context.destroyContext()
        }
    }

    override fun onDestroy() {
        PrefManager.setVoipState(State.IDLE)
    }
}