package com.joshtalks.joshskills.voip.state

import android.content.Context
import android.util.Log
import com.joshtalks.joshskills.voip.Utils
import com.joshtalks.joshskills.voip.communication.constants.ServerConstants
import com.joshtalks.joshskills.voip.communication.model.NetworkAction
import com.joshtalks.joshskills.voip.communication.model.UI
import com.joshtalks.joshskills.voip.constant.RECONNECTED
import com.joshtalks.joshskills.voip.constant.SYNC_UI_STATE
import com.joshtalks.joshskills.voip.constant.UI_STATE_UPDATED
import com.joshtalks.joshskills.voip.inSeconds
import com.joshtalks.joshskills.voip.updateLastCallDetails
import kotlinx.coroutines.*

// Some Temp. Network Problem
class ReconnectingState(val context: CallContext) : VoipState {
    private val TAG = "ReconnectingState"
    private val scope = CoroutineScope(Dispatchers.IO)
    private var listenerJob: Job? = null

    init {
        observe()
    }

    override fun connect() {
        Log.d(TAG, "connect: Illegal Request")
    }

    override fun disconnect() {
        scope.launch {
            context.closeCallScreen()
            moveToLeavingState()
        }
    }

    override fun backPress() {
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
                        RECONNECTED -> {
                            ensureActive()
                            context.reconnected()
                            val uiState = context.currentUiState.copy(isReconnecting = false)
                            context.updateUIState(uiState = uiState)
                            // Emit Event to show Call Screen
                            ensureActive()
                            context.sendEventToUI(event)
                            context.sendMessageToServer(
                                UI(
                                    channelName = context.channelData.getChannel(),
                                    type = ServerConstants.UI_STATE_UPDATED,
                                    isHold = if (context.currentUiState.isOnHold) 1 else 0,
                                    isMute = if (context.currentUiState.isOnMute) 1 else 0,
                                    address = context.channelData.getPartnerMentorId()
                                )
                            )
                            context.state = ConnectedState(context)
                            break@loop
                        }
                        UI_STATE_UPDATED -> {
                            val uiData = event.obj as UI
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
                        else -> throw IllegalEventException("In $TAG but received ${event.what} expected $RECONNECTED")
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
        context.state = LeavingState(context)
        scope.cancel()
    }
}