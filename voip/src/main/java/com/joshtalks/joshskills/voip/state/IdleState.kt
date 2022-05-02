package com.joshtalks.joshskills.voip.state

import android.util.Log

// Can make Calls
class IdleState(val context: CallContext) : VoipState {
    private val TAG = "IdleState"

    override fun connect() {
        Log.d(TAG, "connect: $context")
        context.state = SearchingState(context)
    }

    override fun disconnect() {}

    override fun backPress() {/*Close the Call Screen*/ }

    override fun onDestroy() {}
}