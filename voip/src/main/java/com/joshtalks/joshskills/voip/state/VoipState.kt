package com.joshtalks.joshskills.voip.state

import android.os.SystemClock
import android.util.Log
import com.joshtalks.joshskills.voip.Utils
import com.joshtalks.joshskills.voip.communication.constants.ServerConstants
import com.joshtalks.joshskills.voip.communication.model.UI
import com.joshtalks.joshskills.voip.communication.model.UserAction
import com.joshtalks.joshskills.voip.constant.Event
import com.joshtalks.joshskills.voip.constant.State
import com.joshtalks.joshskills.voip.data.RecordingButtonState
import com.joshtalks.joshskills.voip.data.local.PrefManager
import com.joshtalks.joshskills.voip.mediator.ActionDirection
import com.joshtalks.joshskills.voip.voipanalytics.CallAnalytics
import com.joshtalks.joshskills.voip.voipanalytics.EventName
import kotlinx.coroutines.*

interface VoipState {
    fun connect() {}
    fun disconnect() {}
    fun backPress() {}
    fun onError(reason: String)
    fun onDestroy()
}

//abstract class BaseVoipState(val context: CallContext) : VoipState {
//    protected val ignoringEventSet = mutableSetOf<Event>()
//    protected val eventSet = mutableSetOf<Event>()
//    private val scope = CoroutineScope(Dispatchers.IO)
//    private var listenerJob: Job? = null
//    private val TAG = this::class.java.simpleName
//
//    override fun connect() {
//        super.connect()
//    }
//
//    override fun disconnect() {
//        super.disconnect()
//    }
//
//    override fun backPress() {
//        super.backPress()
//    }
//
//    override fun onError(reason: String) {
//        TODO("Not yet implemented")
//    }
//
//    override fun onDestroy() {
//        TODO("Not yet implemented")
//    }
//
//    private fun observe() {
//        Log.d(TAG, "Started Observing")
//        listenerJob = scope.launch {
//            loop@ while (true) {
//                ensureActive()
//                val event = context.getStreamPipe().receive()
//                Log.d(TAG, "Received after observing : ${event.type}")
//                ensureActive()
//                when (event.type) {
//                    Event.ERROR, Event.CALL_INITIATED_EVENT,
//                    Event.CALL_CONNECTED_EVENT, Event.CALL_DISCONNECTED,
//                    Event.RECONNECTING_FAILED, Event.RECEIVED_CHANNEL_DATA,
//                    Event.RECONNECTED, Event.RECONNECTING-> onVoipCallStateChange(event.type)
//                    Event.REMOTE_USER_DISCONNECTED_AGORA, Event.REMOTE_USER_DISCONNECTED_USER_LEFT,
//                    Event.REMOTE_USER_DISCONNECTED_MESSAGE -> onCallDisconnectRequest(event.type)
//                    Event.SPEAKER_ON_REQUEST, Event.SPEAKER_OFF_REQUEST,
//                    Event.HOLD_REQUEST, Event.UNHOLD_REQUEST,
//                    Event.MUTE_REQUEST, Event.UNMUTE_REQUEST,
//                    Event.TOPIC_IMAGE_CHANGE_REQUEST, Event.TOPIC_IMAGE_RECEIVED -> onUserAction(event.type)
//                    Event.MUTE -> TODO()
//                    Event.UNMUTE -> TODO()
//                    Event.HOLD -> TODO()
//                    Event.UNHOLD -> TODO()
//                    Event.INCOMING_CALL -> TODO()
//                    Event.CLOSE_CALL_SCREEN -> TODO()
//                    Event.SYNC_UI_STATE -> TODO()
//                    Event.UI_STATE_UPDATED -> TODO()
//                }
//            }
//        }
//    }
//
//    /**
//     * 1. Ignore Events
//     * 2. Handle Events
//     * 3. Error Events
//     */
//
//    protected fun onUserAction(action: Event) {
//        when (action) {
//            in ignoringEventSet -> {
//
//            }
//
//            Event.SPEAKER_ON_REQUEST -> {
//                context.enableSpeaker(true)
//                val uiState = context.currentUiState.copy(isSpeakerOn = true)
//                context.updateUIState(uiState = uiState)
//            }
//            Event.SPEAKER_OFF_REQUEST -> {
//                context.enableSpeaker(false)
//                val uiState = context.currentUiState.copy(isSpeakerOn = false)
//                context.updateUIState(uiState = uiState)
//            }
//            Event.MUTE_REQUEST -> {
//                val uiState = context.currentUiState.copy(isOnMute = true)
//                context.updateUIState(uiState = uiState)
//                val userAction = UserAction(
//                    ServerConstants.MUTE,
//                    context.channelData.getChannel(),
//                    address = context.channelData.getPartnerMentorId()
//                )
//                context.changeMicState(true)
//                context.sendMessageToServer(userAction)
//            }
//            Event.UNMUTE_REQUEST -> {
//                val uiState = context.currentUiState.copy(isOnMute = false)
//                context.updateUIState(uiState = uiState)
//                val userAction = UserAction(
//                    ServerConstants.UNMUTE,
//                    context.channelData.getChannel(),
//                    address = context.channelData.getPartnerMentorId()
//                )
//                context.changeMicState(false)
//                context.sendMessageToServer(userAction)
//            }
//            Event.HOLD_REQUEST -> {
//                if (context.currentUiState.recordingButtonState == RecordingButtonState.RECORDING) {
//                    context.startRecording()
//                }
//                val uiState = context.currentUiState.copy(
//                    isOnHold = true,
//                    recordingButtonState = RecordingButtonState.IDLE
//                )
//                context.updateUIState(uiState = uiState)
//                val userAction = UserAction(
//                    ServerConstants.ONHOLD,
//                    context.channelData.getChannel(),
//                    address = context.channelData.getPartnerMentorId()
//                )
//                context.sendMessageToServer(userAction)
//            }
//            Event.UNHOLD_REQUEST -> {
//                val uiState = context.currentUiState.copy(isOnHold = false)
//                context.updateUIState(uiState = uiState)
//                val userAction = UserAction(
//                    ServerConstants.RESUME,
//                    context.channelData.getChannel(),
//                    address = context.channelData.getPartnerMentorId()
//                )
//                context.sendMessageToServer(userAction)
//            }
//            Event.TOPIC_IMAGE_CHANGE_REQUEST -> {
//                val userAction = UserAction(
//                    ServerConstants.TOPIC_IMAGE_REQUEST,
//                    context.channelData.getChannel(),
//                    address = Utils.uuid ?: ""
//                )
//                context.sendMessageToServer(userAction)
//            }
//            Event.TOPIC_IMAGE_RECEIVED -> {
//
//            }
//        }
//    }
//
//    protected fun onRemoteMicStateChange(isMute: Boolean) {
//
//    }
//
//    protected fun onRemotePhoneStateChange(isOnCall: Boolean) {
//
//    }
//
//    protected fun onCallDisconnectRequest(reason: Event) {
//
//    }
//
//    protected fun onVoipCallStateChange(state: Event) {
//
//    }
//}