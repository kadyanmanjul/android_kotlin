package com.joshtalks.joshskills.voip.state

import android.os.Message
import android.os.SystemClock
import android.util.Log
import com.joshtalks.joshskills.voip.communication.constants.ServerConstants
import com.joshtalks.joshskills.voip.communication.model.NetworkAction
import com.joshtalks.joshskills.voip.communication.model.UI
import com.joshtalks.joshskills.voip.communication.model.UserAction
import com.joshtalks.joshskills.voip.constant.Event.*
import com.joshtalks.joshskills.voip.constant.State
import com.joshtalks.joshskills.voip.data.local.PrefManager
import com.joshtalks.joshskills.voip.webrtc.CallState
import com.joshtalks.joshskills.voip.webrtc.Envelope
import com.joshtalks.joshskills.voip.webrtc.USER_DROP_OFFLINE
import com.joshtalks.joshskills.voip.webrtc.USER_QUIT_CHANNEL
import kotlinx.coroutines.*

// User Joined the Agora Channel
class JoinedState(val context: CallContext) : VoipState {
    private val TAG = "JoinedState"
    private val scope = CoroutineScope(Dispatchers.IO + CoroutineExceptionHandler { coroutineContext, throwable ->
        Log.d(TAG, "CoroutineExceptionHandler : $throwable")
        throwable.printStackTrace()
    })
    private var listenerJob: Job? = null

    init {
        Log.d("Call State", TAG)
        observe()
    }

    // Red Button Pressed
    override fun disconnect() {
        Log.d(TAG, "disconnect: Red Button Pressed")
        scope.launch {
            try{
                Log.d(TAG, "disconnect : User Red Press switching to Leaving State")
                context.closeCallScreen()
                moveToLeavingState()
            }
            catch (e : Exception){
                if(e is CancellationException)
                    throw e
                e.printStackTrace()
            }
        }
    }
    // Showing user connecting and then user pressed back
    override fun backPress() {
        Log.d(TAG, "backPress: Ignore")
    }

    override fun onError() {
        disconnect()
    }

    override fun onDestroy() {
        scope.cancel()
    }

    private fun observe() {
        Log.d(TAG, "Started Observing")
        listenerJob = scope.launch {
            try {
                loop@ while (true) {
                    ensureActive()
                    val event = context.getStreamPipe().receive()
                    Log.d(TAG, "Received after observing : ${event.type}")
                    ensureActive()
                    when (event.type) {
                   CALL_CONNECTED_EVENT -> {
                            Log.d(TAG, "observe: Joined Channel --> ${context.channelData.getChannel()}")
                            // Emit Event to show Call Screen
                            ensureActive()
                            val startTime = SystemClock.elapsedRealtime()
                            val uiState = context.currentUiState.copy(startTime = startTime)
                            context.updateUIState(uiState = uiState)
                            ensureActive()
                            val connectedEvent = Envelope(event.type, CallConnectData(
                                startTime,
                                context.channelData.getCallingPartnerName()
                            ))
                            context.sendEventToUI(connectedEvent)
                            PrefManager.setVoipState(State.CONNECTED)
                            context.state = ConnectedState(context)
                       Log.d(TAG, "Received : ${event.type} switched to ${context.state}")

                       break@loop
                        }
                        MUTE -> {
                            ensureActive()
                            val uiState = context.currentUiState.copy(isRemoteUserMuted = true)
                            context.updateUIState(uiState = uiState)
                        }
                        UNMUTE -> {
                            ensureActive()
                            val uiState = context.currentUiState.copy(isRemoteUserMuted = false)
                            context.updateUIState(uiState = uiState)
                        }
                        HOLD -> {
                            ensureActive()
                            val uiState = context.currentUiState.copy(isOnHold = true)
                            context.updateUIState(uiState = uiState)
                        }
                        UNHOLD -> {
                            ensureActive()
                            val uiState = context.currentUiState.copy(isOnHold = false)
                            context.updateUIState(uiState = uiState)
                        }
                        SPEAKER_ON_REQUEST -> {
                            ensureActive()
                            val uiState = context.currentUiState.copy(isSpeakerOn = true)
                            context.updateUIState(uiState = uiState)
                        }
                        SPEAKER_OFF_REQUEST -> {
                            val uiState = context.currentUiState.copy(isSpeakerOn = false)
                            context.updateUIState(uiState = uiState)
                        }
                        MUTE_REQUEST -> {
                            ensureActive()
                            val uiState = context.currentUiState.copy(isOnMute = true)
                            context.updateUIState(uiState = uiState)
                            val userAction = UserAction(
                                ServerConstants.MUTE,
                                context.channelData.getChannel(),
                                address = context.channelData.getPartnerMentorId()
                            )
                            context.changeMicState(true)
                            context.sendMessageToServer(userAction)
                        }
                        UNMUTE_REQUEST -> {
                            ensureActive()
                            val uiState = context.currentUiState.copy(isOnMute = false)
                            context.updateUIState(uiState = uiState)
                            val userAction = UserAction(
                                ServerConstants.UNMUTE,
                                context.channelData.getChannel(),
                                address = context.channelData.getPartnerMentorId()
                            )
                            context.changeMicState(true)
                            context.sendMessageToServer(userAction)
                        }
                        HOLD_REQUEST -> {
                            ensureActive()
                            val uiState = context.currentUiState.copy(isOnHold = true)
                            context.updateUIState(uiState = uiState)
                            val userAction = UserAction(
                                ServerConstants.ONHOLD,
                                context.channelData.getChannel(),
                                address = context.channelData.getPartnerMentorId()
                            )
                            context.sendMessageToServer(userAction)
                        }
                        UNHOLD_REQUEST -> {
                            ensureActive()
                            val uiState = context.currentUiState.copy(isOnHold = false)
                            context.updateUIState(uiState = uiState)
                            val userAction = UserAction(
                                ServerConstants.RESUME,
                                context.channelData.getChannel(),
                                address = context.channelData.getPartnerMentorId()
                            )
                            context.sendMessageToServer(userAction)
                        }
                        SYNC_UI_STATE -> {
                            ensureActive()
                            context.sendMessageToServer(
                                UI(
                                    channelName = context.channelData.getChannel(),
                                    type = ServerConstants.UI_STATE_UPDATED,
                                    isHold = if (context.currentUiState.isOnHold) 1 else 0,
                                    isMute = if (context.currentUiState.isOnMute) 1 else 0,
                                    address = context.channelData.getPartnerMentorId()
                                )
                            )
                        }
                        UI_STATE_UPDATED -> {
                            ensureActive()
                            val uiData = event.data as UI
                            if (uiData.getType() == ServerConstants.UI_STATE_UPDATED)
                                context.sendMessageToServer(
                                    UI(
                                        channelName = context.channelData.getChannel(),
                                        type = ServerConstants.ACK_UI_STATE_UPDATED,
                                        isHold = if (context.currentUiState.isOnHold) 1 else 0,
                                        isMute = if (context.currentUiState.isOnMute) 1 else 0,
                                        address = context.channelData.getPartnerMentorId()
                                    )
                                )
                            val uiState = context.currentUiState.copy(
                                isRemoteUserMuted = uiData.isMute(),
                                isOnHold = uiData.isHold()
                            )
                            context.updateUIState(uiState = uiState)
                        }
                        else -> throw IllegalEventException("In $TAG but received ${event.type} expected $CALL_INITIATED_EVENT")
                    }
                }
                scope.cancel()
            } catch (e: Throwable) {
                if(e is CancellationException)
                    throw e
                else {
                    e.printStackTrace()
                    context.closeCallScreen()
                    Log.d(TAG, "exception : $e switching to Leaving State")
                    moveToLeavingState()
                }
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
        Log.d(TAG, "Received : switched to ${context.state}")
        scope.cancel()
    }
}

data class CallConnectData(val startTime: Long, val userName: String)