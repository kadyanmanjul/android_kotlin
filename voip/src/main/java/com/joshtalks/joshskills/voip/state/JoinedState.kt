package com.joshtalks.joshskills.voip.state

import android.os.Message
import android.os.SystemClock
import android.util.Log
import com.joshtalks.joshskills.voip.communication.constants.ServerConstants
import com.joshtalks.joshskills.voip.communication.model.NetworkAction
import com.joshtalks.joshskills.voip.constant.*
import com.joshtalks.joshskills.voip.data.local.PrefManager
import com.joshtalks.joshskills.voip.mediator.CallingMediator
import kotlinx.coroutines.*

// User Joined the Agora Channel
class JoinedState(val context: CallContext) : VoipState {
    private val TAG = "JoinedState"
    private val scope = CoroutineScope(Dispatchers.IO)
    private var listenerJob : Job? = null

    init {
        Log.d("Call State", TAG)
        observe()
    }

    // Red Button Pressed
    override fun disconnect() {
        Log.d(TAG, "disconnect: Red Button Pressed")
        scope.launch {
            context.closeCallScreen()
            moveToLeavingState()
        }
    }

    // Showing user connecting and then user pressed back
    override fun backPress() {
        Log.d(TAG, "backPress: Ignore")
    }

    override fun onError() { disconnect() }

    override fun onDestroy() {
        scope.cancel()
    }

    private fun observe() {
        listenerJob =  scope.launch {
            try {
                ensureActive()
                val event = context.getStreamPipe().receive()
                ensureActive()
                if (event.what == CALL_CONNECTED_EVENT) {
                    Log.d(TAG, "observe: Joined Channel --> ${context.channelData.getChannel()}")
                    // Emit Event to show Call Screen
                    ensureActive()
                    val startTime = SystemClock.elapsedRealtime()
                    val uiState = context.currentUiState.copy(startTime = startTime)
                    context.updateUIState(uiState = uiState)
                    ensureActive()
                    val connectedEvent = Message.obtain()
                    connectedEvent.copyFrom(event)
                    connectedEvent.obj = CallConnectData(startTime, context.channelData.getCallingPartnerName())
                    context.sendEventToUI(connectedEvent)
                    PrefManager.setVoipState(State.CONNECTED)
                    context.state = ConnectedState(context)
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

data class CallConnectData(val startTime : Long, val userName : String)