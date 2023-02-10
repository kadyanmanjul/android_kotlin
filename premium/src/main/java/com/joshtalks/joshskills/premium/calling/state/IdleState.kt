package com.joshtalks.joshskills.premium.calling.state

import android.util.Log
import com.joshtalks.joshskills.premium.calling.constant.State
import com.joshtalks.joshskills.premium.calling.data.local.PrefManager
import com.joshtalks.joshskills.premium.calling.voipanalytics.CallAnalytics
import com.joshtalks.joshskills.premium.calling.voipanalytics.EventName
import kotlinx.coroutines.CancellationException
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

    override fun onError(reason: String) {
        CallAnalytics.addAnalytics(
            event = EventName.ON_ERROR,
            agoraMentorId = "-1",
            extra = "In $TAG : $reason"
        )
        scope.launch {
            try{
                Log.d(TAG, "onError call screen close and context destroy")
                context.closeCallScreen()
                context.closePipe()
                onDestroy()
            }
            catch (e : Exception){
                if(e is CancellationException)
                    throw e
                e.printStackTrace()
            }
        }
    }

    override fun onDestroy() {
        Log.d(TAG, "Voip State set onDestroy")
        PrefManager.setVoipState(State.IDLE)
    }
}