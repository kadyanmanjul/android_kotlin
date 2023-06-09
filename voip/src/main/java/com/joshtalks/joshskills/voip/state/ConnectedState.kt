package com.joshtalks.joshskills.voip.state

import android.util.Log
import com.joshtalks.joshskills.base.constants.INTENT_DATA_EXPERT_PRICE_PER_MIN
import com.joshtalks.joshskills.base.constants.INTENT_DATA_TOTAL_AMOUNT
import com.joshtalks.joshskills.voip.ProximityHelper
import com.joshtalks.joshskills.voip.Utils
import com.joshtalks.joshskills.voip.communication.constants.ServerConstants
import com.joshtalks.joshskills.voip.communication.model.NetworkAction
import com.joshtalks.joshskills.voip.communication.model.UI
import com.joshtalks.joshskills.voip.communication.model.UserAction
import com.joshtalks.joshskills.voip.constant.Event.*
import com.joshtalks.joshskills.voip.constant.State
import com.joshtalks.joshskills.voip.data.local.PrefManager
import com.joshtalks.joshskills.voip.inSeconds
import com.joshtalks.joshskills.voip.mediator.ActionDirection
import com.joshtalks.joshskills.voip.updateLastCallDetails
import com.joshtalks.joshskills.voip.voipanalytics.CallAnalytics
import com.joshtalks.joshskills.voip.voipanalytics.EventName
import kotlinx.coroutines.*

// Remote User Joined the Channel and can talk
class ConnectedState(val context: CallContext) : VoipState {
    private val TAG = "ConnectedState"
    private val scope = CoroutineScope(Dispatchers.IO)
    private var listenerJob: Job? = null
    private var paymentJob: Job? = null

    init {
        Log.d("Call State", TAG)
        observe()
        Utils.context?.let { ProximityHelper.getInstance(it)?.start() }
        context.audioController?.activate()
    }

    // Red Button Pressed
    override fun disconnect() {
        Log.d(TAG, "disconnect : User Red Press switching to Leaving State")
        moveToLeavingState()
    }

    fun startPaymentTimer(coin : Int, rate : Int) {
        paymentJob = scope.launch {
            // TODO: Coin Logic
            var secondsLeft :Long= (((coin / rate) * 60) * 1000).toLong()
            //var secondsLeft = coin * rate
            while (true) {
                delay(secondsLeft)
                secondsLeft--

                // TODO: Save to Local (Reverse operation of line 121)
                if(secondsLeft <= 0)
                    break
            }
        }
    }

    fun stopPaymentTimer() {
        paymentJob?.cancel()
    }


    override fun backPress() {
        // Do nothing because users talking
        Log.d(TAG, "backPress: ")
        CallAnalytics.addAnalytics(
            event = EventName.BACK_PRESSED,
            agoraCallId = context.channelData.getCallingId().toString(),
            agoraMentorId = context.channelData.getAgoraUid().toString(),
            extra = TAG
        )
    }

    override fun onError(reason: String) {
        CallAnalytics.addAnalytics(
            event = EventName.ON_ERROR,
            agoraCallId = context.channelData.getCallingId().toString(),
            agoraMentorId = context.channelData.getAgoraUid().toString(),
            extra = "In $TAG : $reason"
        )
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
                        TOPIC_IMAGE_RECEIVED -> {
                            ensureActive()
                            val uiState = context.currentUiState.copy(currentTopicImage = event.data.toString())
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
                        TOPIC_IMAGE_CHANGE_REQUEST -> {
                            ensureActive()
                            val userAction = UserAction(
                                ServerConstants.TOPIC_IMAGE_REQUEST,
                                context.channelData.getChannel(),
                                address = Utils.uuid ?: ""
                            )
                            context.sendMessageToServer(userAction)
                        }
                        SYNC_UI_STATE -> {
                            ensureActive()
                            context.sendMessageToServer(
                                UI(
                                    channelName = context.channelData.getChannel(),
                                    type = ServerConstants.ACK_UI_STATE_UPDATED,
                                    isHold = if (context.currentUiState.isOnHold) 1 else 0,
                                    isMute = if (context.currentUiState.isOnMute) 1 else 0,
                                    address = context.channelData.getPartnerMentorId(),
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
                                        address = context.channelData.getPartnerMentorId(),
                                    )
                                )
                            val uiState = context.currentUiState.copy(
                                isRemoteUserMuted = uiData.isMute(),
                                isOnHold = uiData.isHold(),
                            )
                            context.updateUIState(uiState = uiState)
                        }
                        REMOTE_USER_DISCONNECTED_AGORA -> {
                            ensureActive()
                            Log.d(TAG, "Received : ${event.type} switched to Leaving State")
                            moveToLeavingState()
                        }
                        REMOTE_USER_DISCONNECTED_USER_LEFT -> {
                            CallAnalytics.addAnalytics(
                                event = EventName.DISCONNECTED_BY_AGORA_USER_OFFLINE,
                                agoraCallId = context.channelData.getCallingId().toString(),
                                agoraMentorId = context.channelData.getAgoraUid().toString(),
                                extra = TAG
                            )
                            ensureActive()
                            Log.d(TAG, "Received : ${event.type} switched to Leaving State")
                            moveToLeavingState()
                        }
                        REMOTE_USER_DISCONNECTED_MESSAGE -> {
                            CallAnalytics.addAnalytics(
                                event = EventName.DISCONNECTED_BY_REMOTE_USER,
                                agoraCallId = context.channelData.getCallingId().toString(),
                                agoraMentorId = context.channelData.getAgoraUid().toString(),
                                extra = TAG
                            )
                            ensureActive()
                            Log.d(TAG, "Received : ${event.type} switched to Leaving State")
                            moveToLeavingState()
                        }
                        RECONNECTED, RECEIVED_CHANNEL_DATA -> {
                            // Ignore Error Event from Agora
                            val msg = "Ignoring : In $TAG but received ${event.type} event don't know how to process"
                            CallAnalytics.addAnalytics(
                                event = EventName.ILLEGAL_EVENT_RECEIVED,
                                agoraCallId = context.channelData.getCallingId().toString(),
                                agoraMentorId = context.channelData.getAgoraUid().toString(),
                                extra = msg
                            )
                            Log.d(TAG, "Ignoring : In $TAG but received ${event.type} event don't know how to process")
                        }
                        else -> {
                            val msg = "In $TAG but received ${event.type} event don't know how to process"
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
                    Log.d(TAG, "disconnect : Exception $e switching to Leaving State")
                    moveToLeavingState()
                }
            }
        }
    }

    private fun moveToLeavingState() {
        context.audioController?.deactivate()
        scope.launch {
            try {
                listenerJob?.cancel()
                context.closeCallScreen()
               // stopPaymentTimer()
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
                    duration = context.durationInMillis,
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
            } catch (e: Exception) {
                if (e is CancellationException)
                    throw e
                e.printStackTrace()
            }
        }
    }
}