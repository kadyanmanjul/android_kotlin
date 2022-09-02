package com.joshtalks.joshskills.voip.state

import android.util.Log
import com.joshtalks.joshskills.voip.Utils
import com.joshtalks.joshskills.voip.communication.constants.ServerConstants
import com.joshtalks.joshskills.voip.communication.model.IncomingGameNextWord
import com.joshtalks.joshskills.voip.communication.model.NetworkAction
import com.joshtalks.joshskills.voip.communication.model.UI
import com.joshtalks.joshskills.voip.communication.model.UserAction
import com.joshtalks.joshskills.voip.constant.Event
import com.joshtalks.joshskills.voip.constant.Event.*
import com.joshtalks.joshskills.voip.constant.State
import com.joshtalks.joshskills.voip.data.local.PrefManager
import com.joshtalks.joshskills.voip.mediator.ActionDirection
import com.joshtalks.joshskills.voip.voipanalytics.CallAnalytics
import com.joshtalks.joshskills.voip.voipanalytics.EventName
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

// Got a Channel and Joining Agora State
class JoiningState(val context: CallContext) : VoipState {
    private val TAG = "JoiningState"
    private val scope = CoroutineScope(Dispatchers.IO + CoroutineExceptionHandler { coroutineContext, throwable ->
        Log.d(TAG, "CoroutineExceptionHandler : $throwable")
        throwable.printStackTrace()
    })
    @OptIn(DelicateCoroutinesApi::class)
    private val webrtcScope = CoroutineScope(newSingleThreadContext("Webrtc Context") + CoroutineExceptionHandler { coroutineContext, throwable ->
        Log.d(TAG, "CoroutineExceptionHandler : $throwable")
        throwable.printStackTrace()
    })
    val mutex = Mutex(false)

    init {
        Log.d("Call State", TAG)
        webrtcScope.launch {
            mutex.withLock {
                observe()
                context.joinChannel(context.channelData)
            }
        }

        CallAnalytics.addAnalytics(
            event = EventName.CHANNEL_JOINING,
            agoraCallId = context.channelData.getCallingId().toString(),
            agoraMentorId = context.channelData.getAgoraUid().toString()
        )
    }

    override fun disconnect() {
        scope.launch {
            context.closeCallScreen()
            moveToLeavingState()
        }
    }

    // Join Channel Already Called
    override fun backPress() {
        CallAnalytics.addAnalytics(
            event = EventName.BACK_PRESSED,
            agoraCallId = context.channelData.getCallingId().toString(),
            agoraMentorId = context.channelData.getAgoraUid().toString(),
            extra = TAG
        )
        moveToLeavingState()
    }

    override fun onError(reason: String) {
        CallAnalytics.addAnalytics(
            event = EventName.ON_ERROR,
            agoraCallId = context.channelData.getCallingId().toString(),
            agoraMentorId = context.channelData.getAgoraUid().toString(),
            extra = "In $TAG : $reason"
        )
        moveToLeavingState()
    }

    override fun onDestroy() {
        scope.cancel()
        webrtcScope.cancel()
    }

    private fun observe() {
        scope.launch {
            Log.d(TAG, "Started Observing")
            try {
                loop@ while (true) {
                    ensureActive()
                    val event = context.getStreamPipe().receive()
                    Log.d(TAG, "Received after observing : ${event.type}")
                    ensureActive()
                    when(event.type){
                        CALL_INITIATED_EVENT ->{
                            Log.d(TAG, "observe: Joined Channel --> ${context.channelData.getChannel()}")
                            // Emit Event to show Call Screen
                            ensureActive()
                            context.sendEventToUI(event)
                            PrefManager.setVoipState(State.JOINED)
                            context.state = JoinedState(context)
                            Log.d(TAG, "Received : ${event.type} switched to ${context.state}")
                            break@loop
                        }
                        // TODO: Not a fix
                        RECEIVED_CHANNEL_DATA -> {
                            // Ignore
                            val msg = "In $TAG but received ${event.type} { with channel - ${context.channelData.getChannel()} } expected $CALL_INITIATED_EVENT"
                            CallAnalytics.addAnalytics(
                                event = EventName.ILLEGAL_EVENT_RECEIVED,
                                agoraCallId = context.channelData.getCallingId().toString(),
                                agoraMentorId = context.channelData.getAgoraUid().toString(),
                                extra = msg
                            )
                        }
                        REMOTE_USER_DISCONNECTED_MESSAGE, REMOTE_USER_DISCONNECTED_AGORA, REMOTE_USER_DISCONNECTED_USER_LEFT,
                                Event.START_RECORDING, Event.STOP_RECORDING, Event.CALL_RECORDING_ACCEPT, Event.CALL_RECORDING_REJECT,
                                Event.CANCEL_RECORDING_REQUEST,STOP_GAME_RECORDING,START_GAME_RECORDING -> {
                            // Ignore Error Event from Agora
                            val msg = "Ignoring : In $TAG but received ${event.type} expected $CALL_INITIATED_EVENT"
                            CallAnalytics.addAnalytics(
                                event = EventName.ILLEGAL_EVENT_RECEIVED,
                                agoraCallId = context.channelData.getCallingId().toString(),
                                agoraMentorId = context.channelData.getAgoraUid().toString(),
                                extra = msg
                            )
                            Log.d(TAG, "Ignoring : In $TAG but received ${event.type} expected $CALL_INITIATED_EVENT")
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
                        Event.START_GAME -> {
                            ensureActive()
                            val userAction = UserAction(
                                ServerConstants.START_GAME,
                                context.channelData.getChannel(),
                                address = Utils.uuid ?: ""
                            )
                            val uiState = context.currentUiState.copy(isStartGameClicked = true)
                            context.updateUIState(uiState = uiState)
                            context.sendMessageToServer(userAction)
                        }
                        Event.END_GAME -> {
                            ensureActive()
                            if(event.data == ActionDirection.SERVER) {
                                val userAction = UserAction(
                                    ServerConstants.END_GAME,
                                    context.channelData.getChannel(),
                                    address = context.channelData.getPartnerMentorId()
                                )
                                context.sendMessageToServer(userAction)
                            }
                            val uiState = context.currentUiState.copy(isStartGameClicked = false, isNextWordClicked = false, nextGameWord = "", isRemoteUserGameStarted = false)
                            context.updateUIState(uiState = uiState)
                        }
                        Event.NEXT_WORD_REQUEST -> {
                            ensureActive()
                            val userAction = UserAction(
                                ServerConstants.NEXT_WORD_REQUEST,
                                context.channelData.getChannel(),
                                address = Utils.uuid ?: ""
                            )
                            context.sendMessageToServer(userAction)

                            val uiState = context.currentUiState.copy(isNextWordClicked = true)
                            context.updateUIState(uiState = uiState)
                        }
                        Event.NEXT_WORD_RECEIVED -> {
                            ensureActive()
                            val incomingWord= event.data as IncomingGameNextWord
                            val uiState = context.currentUiState.copy(nextGameWord = incomingWord.word, nextGameWordColor = incomingWord.color, isNextWordClicked = false)
                            context.updateUIState(uiState = uiState)

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
                                    isPlayButtonClick = if(context.currentUiState.isStartGameClicked) 1 else 0
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
                                        isPlayButtonClick = if(context.currentUiState.isStartGameClicked) 1 else 0
                                    )
                                )
                            val uiState = context.currentUiState.copy(
                                isRemoteUserMuted = uiData.isMute(),
                                isOnHold = uiData.isHold(),
                                isRemoteUserGameStarted = uiData.isPlayBtnClick()
                            )
                            context.updateUIState(uiState = uiState)
                        }
                        else -> {
                            val msg = "In $TAG but received ${event.type} expected $CALL_INITIATED_EVENT"
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
                webrtcScope.cancel()
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
        scope.cancel()
        val networkAction = NetworkAction(
            channelName = context.channelData.getChannel(),
            uid = context.channelData.getAgoraUid(),
            type = ServerConstants.DISCONNECTED,
            duration = 0,
            address = context.channelData.getPartnerMentorId()
        )
        context.sendMessageToServer(networkAction)
        webrtcScope.launch {
            try {
                mutex.withLock {
                    context.disconnectCall()
                    PrefManager.setVoipState(State.LEAVING)
                    context.state = LeavingState(context)
                    Log.d(TAG, "Received : switched to ${context.state}")
                    webrtcScope.cancel()
                }
            } catch (e : Exception){
                if(e is CancellationException)
                    throw e
                e.printStackTrace()
            }
        }
    }
}