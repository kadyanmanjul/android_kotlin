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
import com.joshtalks.joshskills.voip.voipanalytics.CallAnalytics
import com.joshtalks.joshskills.voip.voipanalytics.EventName
import kotlinx.coroutines.*

// Some Temp. Network Problem
class ReconnectingState(val context: CallContext) : VoipState {
    private val TAG = "ReconnectingState"
    private val scope = CoroutineScope(Dispatchers.IO)
    private var listenerJob: Job? = null

    init {
        Log.d("Call State", TAG)
        observe()
    }

    override fun toString(): String {
        return this::class.simpleName ?: ""
    }

    override fun connect() {
        Log.d(TAG, "connect: Illegal Request")
    }

    override fun disconnect() {
        scope.launch {
            try{
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

    override fun backPress() {
        Log.d(TAG, "backPress: ")
        CallAnalytics.addAnalytics(
            event = EventName.BACK_PRESSED,
            agoraCallId = context.channelData.getCallingId().toString(),
            agoraMentorId = context.channelData.getAgoraUid().toString(),
            extra = TAG
        )
    }

    override fun onError() {
        CallAnalytics.addAnalytics(
            event = EventName.ON_ERROR,
            agoraCallId = context.channelData.getCallingId().toString(),
            agoraMentorId = context.channelData.getAgoraUid().toString(),
            extra = TAG
        )
        disconnect()
    }

    override fun onDestroy() {
        scope.cancel()
    }

    // Handle Events related to Reconnecting State
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
                            PrefManager.setVoipState(State.CONNECTED)
                            context.state = ConnectedState(context)
                            Log.d(TAG, "Received : ${event.type} switched to ${context.state}")
                            break@loop
                        }
                        UI_STATE_UPDATED -> {
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
                        SPEAKER_ON_REQUEST -> {
                            ensureActive()
                            context.enableSpeaker(true)
                            val uiState = context.currentUiState.copy(isSpeakerOn = true)
                            context.updateUIState(uiState = uiState)
                        }
                        SPEAKER_OFF_REQUEST -> {
                            context.enableSpeaker(false)
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
                        TOPIC_IMAGE_RECEIVED -> {
                            ensureActive()
                            val uiState = context.currentUiState.copy(currentTopicImage = event.data.toString())
                            context.updateUIState(uiState = uiState)
                        }
                        TOPIC_IMAGE_CHANGE_REQUEST ->{
                            ensureActive()
                            val userAction = UserAction(
                                ServerConstants.TOPIC_IMAGE_REQUEST,
                                context.channelData.getChannel(),
                                address = Utils.uuid ?: ""
                            )
                            context.sendMessageToServer(userAction)
                        }
                        REMOTE_USER_DISCONNECTED_AGORA -> {
                            Log.d(TAG, "observe: disconnect event ${event.type}")
                            ensureActive()
                            moveToLeavingState()
                        }
                        REMOTE_USER_DISCONNECTED_USER_LEFT -> {
                            CallAnalytics.addAnalytics(
                                event = EventName.DISCONNECTED_BY_AGORA_USER_OFFLINE,
                                agoraCallId = context.channelData.getCallingId().toString(),
                                agoraMentorId = context.channelData.getAgoraUid().toString(),
                                extra = TAG
                            )
                            Log.d(TAG, "observe: disconnect event ${event.type}")
                            ensureActive()
                            moveToLeavingState()
                        }
                        REMOTE_USER_DISCONNECTED_MESSAGE -> {
                            CallAnalytics.addAnalytics(
                                event = EventName.DISCONNECTED_BY_REMOTE_USER,
                                agoraCallId = context.channelData.getCallingId().toString(),
                                agoraMentorId = context.channelData.getAgoraUid().toString(),
                                extra = TAG
                            )
                            Log.d(TAG, "observe: disconnect event ${event.type}")
                            ensureActive()
                            moveToLeavingState()
                        }
                        RECONNECTING-> {
                            val msg = "Ignoring : In $TAG but received ${event.type} expected $RECONNECTED"
                            CallAnalytics.addAnalytics(
                                event = EventName.ILLEGAL_EVENT_RECEIVED,
                                agoraCallId = context.channelData.getCallingId().toString(),
                                agoraMentorId = context.channelData.getAgoraUid().toString(),
                                extra = msg
                            )
                            Log.d(TAG, "Ignoring : In $TAG but received ${event.type} expected $RECONNECTED")
                        }
                        else -> {
                            val msg = "In $TAG but received ${event.type} expected $RECONNECTED"
                            CallAnalytics.addAnalytics(
                                event = EventName.ILLEGAL_EVENT_RECEIVED,
                                agoraCallId = context.channelData.getCallingId().toString(),
                                agoraMentorId = context.channelData.getAgoraUid().toString(),
                                extra = msg
                            )
                            throw IllegalEventException(msg)
                        }
                    }

                }
                scope.cancel()
            } catch (e: Throwable) {
                if (e is CancellationException)
                    throw e
                else {
                    e.printStackTrace()
                    context.closeCallScreen()
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
                    topicName = context.channelData.getCallingTopic(),
                    remotesUserMentorId = context.channelData.getPartnerMentorId()
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