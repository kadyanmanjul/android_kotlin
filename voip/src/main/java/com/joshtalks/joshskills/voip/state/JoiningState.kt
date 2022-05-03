package com.joshtalks.joshskills.voip.state

import android.util.Log
import com.joshtalks.joshskills.voip.communication.constants.ServerConstants
import com.joshtalks.joshskills.voip.communication.model.NetworkAction
import com.joshtalks.joshskills.voip.constant.CALL_INITIATED_EVENT
import com.joshtalks.joshskills.voip.constant.JOINED
import com.joshtalks.joshskills.voip.constant.LEAVING
import com.joshtalks.joshskills.voip.constant.State
import com.joshtalks.joshskills.voip.data.local.PrefManager
import kotlinx.coroutines.*

// Got a Channel and Joining Agora State
class JoiningState(val context: CallContext) : VoipState {
    private val TAG = "JoiningState"
    private val scope = CoroutineScope(Dispatchers.IO)
    private var listenerJob : Job? = null

    init {
        Log.d("Call State", TAG)
        observe()
        context.joinChannel(context.channelData)
    }

    // Join Channel Already Called
    override fun backPress() { moveToLeavingState() }

    override fun onError() { backPress() }

    override fun onDestroy() {
        scope.cancel()
    }

    private fun observe() {
        listenerJob =  scope.launch {
            try {
                ensureActive()
                val event = context.getStreamPipe().receive()
                ensureActive()
                if (event.what == CALL_INITIATED_EVENT) {
                    Log.d(TAG, "observe: Joined Channel --> ${context.channelData.getChannel()}")
                    // Emit Event to show Call Screen
                    ensureActive()
                    context.sendEventToUI(event)
                    PrefManager.setVoipState(State.JOINED)
                    context.state = JoinedState(context)
                } else
                    throw IllegalEventException("In $TAG but received ${event.what} expected $CALL_INITIATED_EVENT")
                // TODO: Handle Error Case
                scope.cancel()
            } catch (e: Exception) {
                e.printStackTrace()
                context.closeCallScreen()
                moveToLeavingState()
            }
        }
    }

    private fun moveToLeavingState() {
        listenerJob?.cancel()
        val networkAction = NetworkAction(
            channelName = context.channelData.getChannel(),
            uid = context.channelData.getAgoraUid(),
            type = ServerConstants.DISCONNECTED,
            duration = 0,
            address = context.channelData.getPartnerMentorId()
        )
        context.sendMessageToServer(networkAction)
        context.disconnectCall()
        PrefManager.setVoipState(State.LEAVING)
        context.state = LeavingState(context)
        scope.cancel()
    }
}