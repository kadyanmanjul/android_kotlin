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
        Log.d(TAG, "on Connect Call :  switched to ${context.state}")
    }

    override fun onError() {
        scope.launch {
            Log.d(TAG, "onError call screen close and context destroy")
            context.closeCallScreen()
            context.destroyContext()
        }
    }

    override fun onDestroy() {
        Log.d(TAG, "Voip State set onDestroy")
        PrefManager.setVoipState(State.IDLE)
    }
}