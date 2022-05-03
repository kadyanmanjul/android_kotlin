package com.joshtalks.joshskills.voip.state

import android.util.Log
import com.joshtalks.joshskills.voip.Utils
import com.joshtalks.joshskills.voip.communication.constants.ServerConstants
import com.joshtalks.joshskills.voip.communication.model.NetworkAction
import com.joshtalks.joshskills.voip.communication.model.UI
import com.joshtalks.joshskills.voip.communication.model.UserAction
import com.joshtalks.joshskills.voip.constant.*
import com.joshtalks.joshskills.voip.data.local.PrefManager
import com.joshtalks.joshskills.voip.inSeconds
import com.joshtalks.joshskills.voip.updateLastCallDetails
import com.joshtalks.joshskills.voip.voipLog
import kotlinx.coroutines.*

// Remote User Joined the Channel and can talk
class ConnectedState(val context: CallContext) : VoipState {
    private val TAG = "ConnectedState"
    private val scope = CoroutineScope(Dispatchers.IO)
    private var listenerJob : Job? = null

    init { observe() }

    // Red Button Pressed
    override fun disconnect() {
        scope.launch {
            context.closeCallScreen()
            moveToLeavingState()
        }
    }


    override fun backPress() {
        // Do nothing because users talking
        Log.d(TAG, "backPress: ")
    }

    override fun onDestroy() {
        scope.cancel()
    }

    // Handle Events related to Connected State
    private fun observe() {
        listenerJob = scope.launch {
            try {
                loop@ while (true) {
                    ensureActive()
                    val event = context.getStreamPipe().receive()
                    ensureActive()
                    when (event.what) {
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
                            PrefManager.setVoipState(RECONNECTING)
                            context.state = ReconnectingState(context)
                            break@loop
                        }
                        SPEAKER_ON_REQUEST -> {

                        }
                        SPEAKER_OFF_REQUEST -> {

                        }
                        MUTE_REQUEST -> {
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
                            val uiData = event.obj as UI
                            if(uiData.getType() == ServerConstants.UI_STATE_UPDATED)
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
                        // From Backend
                        REMOTE_USER_DISCONNECTED_AGORA, REMOTE_USER_DISCONNECTED_USER_DROP -> {
                            ensureActive()
                            moveToLeavingState()
                        }
                        else -> throw IllegalEventException("In $TAG but received ${event.what} event don't know how to process")
                    }
                }
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
        PrefManager.setVoipState(LEAVING)
        context.state = LeavingState(context)
        scope.cancel()
    }
}