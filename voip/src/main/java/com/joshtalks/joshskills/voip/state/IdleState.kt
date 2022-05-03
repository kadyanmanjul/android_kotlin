package com.joshtalks.joshskills.voip.state

import android.util.Log
import com.joshtalks.joshskills.voip.constant.IDLE
import com.joshtalks.joshskills.voip.data.local.PrefManager

// Can make Calls
class IdleState(val context: CallContext) : VoipState {
    private val TAG = "IdleState"

    override fun connect() {
        Log.d(TAG, "connect: $context")
        PrefManager.setVoipState(IDLE)
        context.state = SearchingState(context)
    }

    override fun disconnect() {}

    override fun backPress() {/*Close the Call Screen*/ }

    override fun onDestroy() {}
}