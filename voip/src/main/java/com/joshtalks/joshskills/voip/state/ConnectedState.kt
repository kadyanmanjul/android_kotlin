package com.joshtalks.joshskills.voip.state

import android.util.Log
import com.joshtalks.joshskills.voip.Utils
import com.joshtalks.joshskills.voip.communication.constants.ServerConstants
import com.joshtalks.joshskills.voip.communication.model.NetworkAction
import com.joshtalks.joshskills.voip.communication.model.UI
import com.joshtalks.joshskills.voip.communication.model.UserAction
import com.joshtalks.joshskills.voip.constant.Event.*
import com.joshtalks.joshskills.voip.constant.State
import com.joshtalks.joshskills.voip.data.local.PrefManager
import com.joshtalks.joshskills.voip.inSeconds
import com.joshtalks.joshskills.voip.updateLastCallDetails
import kotlinx.coroutines.*

// Remote User Joined the Channel and can talk
class ConnectedState(val context: CallContext) : VoipState {
    private val TAG = "ConnectedState"
    private val scope = CoroutineScope(Dispatchers.IO)
    private var listenerJob: Job? = null

    init {
        Log.d("Call State", TAG)
        observe()
    }

    // Red Button Pressed
    override fun disconnect() {
        Log.d(TAG, "disconnect : User Red Press switching to Leaving State")
        moveToLeavingState()
    }


    override fun backPress() {
        // Do nothing because users talking
        Log.d(TAG, "backPress: ")
    }

    override fun onError() {
        disconnect()
    }

    override fun onDestroy() {
        scope.cancel()
    }

    // Handle Events related to Connected State
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
                        RECONNECTING -> {
                            ensureActive()
                            val uiState = context.currentUiState.copy(isReconnecting = true)
                            context.updateUIState(uiState = uiState)
                            context.reconnecting()
                            PrefManager.setVoipState(State.RECONNECTING)
                            context.state = ReconnectingState(context)
                            Log.d(TAG, "Received : ${event.type} switched to ${context.state}")
                            break@loop
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
                            context.changeMicState(false)
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
                        REMOTE_USER_DISCONNECTED_AGORA, REMOTE_USER_DISCONNECTED_USER_LEFT, REMOTE_USER_DISCONNECTED_MESSAGE -> {
                            ensureActive()
                            Log.d(TAG, "Received : ${event.type} switched to Leaving State")
                            moveToLeavingState()
                        }
                        RECONNECTED -> {
                            // Ignore this event as we are already in Connected State
                        }
                        else -> throw IllegalEventException("In $TAG but received ${event.type} event don't know how to process")
                    }
                }
                scope.cancel()
            } catch (e: Throwable) {
                if (e is CancellationException)
                    throw e
                else {
                    e.printStackTrace()
                    Log.d(TAG, "disconnect : Exception $e switching to Leaving State")
                    moveToLeavingState()
                }
            }
        }
    }

    private fun moveToLeavingState() {
        scope.launch {
            try{
                listenerJob?.cancel()
                context.closeCallScreen()
                Log.d(TAG, "moveToLeavingState: after close screen")
                val networkAction = NetworkAction(
                    channelName = context.channelData.getChannel(),
                    uid = context.channelData.getAgoraUid(),
                    type = ServerConstants.DISCONNECTED,
                    duration = context.durationInMillis.inSeconds(),
                    address = context.channelData.getPartnerMentorId()
                )
                context.sendMessageToServer(networkAction)
                // Show Dialog
                Utils.context?.updateLastCallDetails(
                    duration = context.durationInMillis.inSeconds(),
                    remoteUserName = context.channelData.getCallingPartnerName(),
                    remoteUserImage = context.channelData.getCallingPartnerImage(),
                    callId = context.channelData.getCallingId(),
                    callType = context.callType,
                    remoteUserAgoraId = context.channelData.getPartnerUid(),
                    localUserAgoraId = context.channelData.getAgoraUid(),
                    channelName = context.channelData.getChannel(),
                    topicName = context.channelData.getCallingTopic()
                )
                context.disconnectCall()
                PrefManager.setVoipState(State.LEAVING)
                context.state = LeavingState(context)
                Log.d(TAG, "Received : switched to ${context.state}")
                scope.cancel()
            }
            catch (e : Exception){
                if(e is CancellationException)
                    throw e
                e.printStackTrace()
            }
        }
    }
}