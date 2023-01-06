package com.joshtalks.joshskills.voip.state

import android.os.SystemClock
import android.util.Log
import com.joshtalks.joshskills.voip.Utils
import com.joshtalks.joshskills.voip.communication.constants.ServerConstants
import com.joshtalks.joshskills.voip.communication.model.*
import com.joshtalks.joshskills.voip.constant.Event
import com.joshtalks.joshskills.voip.constant.Event.CALL_CONNECTED_EVENT
import com.joshtalks.joshskills.voip.constant.Event.CALL_DISCONNECTED
import com.joshtalks.joshskills.voip.constant.Event.HOLD
import com.joshtalks.joshskills.voip.constant.Event.HOLD_REQUEST
import com.joshtalks.joshskills.voip.constant.Event.MUTE
import com.joshtalks.joshskills.voip.constant.Event.MUTE_REQUEST
import com.joshtalks.joshskills.voip.constant.Event.RECEIVED_CHANNEL_DATA
import com.joshtalks.joshskills.voip.constant.Event.RECONNECTED
import com.joshtalks.joshskills.voip.constant.Event.RECONNECTING
import com.joshtalks.joshskills.voip.constant.Event.REMOTE_USER_DISCONNECTED_AGORA
import com.joshtalks.joshskills.voip.constant.Event.REMOTE_USER_DISCONNECTED_MESSAGE
import com.joshtalks.joshskills.voip.constant.Event.REMOTE_USER_DISCONNECTED_USER_LEFT
import com.joshtalks.joshskills.voip.constant.Event.SPEAKER_OFF_REQUEST
import com.joshtalks.joshskills.voip.constant.Event.SPEAKER_ON_REQUEST
import com.joshtalks.joshskills.voip.constant.Event.SYNC_UI_STATE
import com.joshtalks.joshskills.voip.constant.Event.TOPIC_IMAGE_CHANGE_REQUEST
import com.joshtalks.joshskills.voip.constant.Event.TOPIC_IMAGE_RECEIVED
import com.joshtalks.joshskills.voip.constant.Event.UI_STATE_UPDATED
import com.joshtalks.joshskills.voip.constant.Event.UNHOLD
import com.joshtalks.joshskills.voip.constant.Event.UNHOLD_REQUEST
import com.joshtalks.joshskills.voip.constant.Event.UNMUTE
import com.joshtalks.joshskills.voip.constant.Event.UNMUTE_REQUEST
import com.joshtalks.joshskills.voip.constant.State
import com.joshtalks.joshskills.voip.data.local.PrefManager
import com.joshtalks.joshskills.voip.mediator.CallDirection
import com.joshtalks.joshskills.voip.voipanalytics.CallAnalytics
import com.joshtalks.joshskills.voip.voipanalytics.EventName
import com.joshtalks.joshskills.voip.webrtc.Envelope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch

// User Joined the Agora Channel
const val CONNECTING_TIMER = 10 * 1000L
const val RETRY_TIMER = 5 * 1000L

class JoinedState(val context: CallContext) : VoipState {
    private val TAG = "JoinedState"
    private val scope = CoroutineScope(Dispatchers.IO + CoroutineExceptionHandler { coroutineContext, throwable ->
        Log.d(TAG, "CoroutineExceptionHandler : $throwable")
        throwable.printStackTrace()
    })

    private var listenerJob: Job? = null
    private var disconnectListenerJob: Job? = null
    private val connectingTimer by lazy {
        scope.launch(start = CoroutineStart.LAZY) {
            try{
                if(context.direction != CallDirection.INCOMING && context.channelData.isNewSearchingEnabled()) {
                    Log.d(TAG, "Retry Timer Started")
                    delay(RETRY_TIMER)
                    ensureActive()
                    listenerJob?.cancel()
                    context.channelData = (context.channelData as Channel).removeChannel()
                    startDisconnectListener()
                    context.disconnectCall()
                } else {
                    Log.d(TAG, "Connecting Timer Started")
                    delay(CONNECTING_TIMER)
                    ensureActive()
                    CallAnalytics.addAnalytics(
                        event = EventName.DISCONNECTED_BY_CONNECTING_TIMEOUT,
                        agoraCallId = context.channelData.getCallingId().toString(),
                        agoraMentorId = context.channelData.getAgoraUid().toString(),
                        extra = TAG
                    )
                    context.closeCallScreen()
                    moveToLeavingState()
                }
            }
            catch (e : Exception){
                if(e is CancellationException)
                    throw e
                e.printStackTrace()
            }
        }
    }

    init {
        Log.d("Call State", TAG)
        CallAnalytics.addAnalytics(
            event = EventName.CHANNEL_JOINED,
            agoraCallId = context.channelData.getCallingId().toString(),
            agoraMentorId = context.channelData.getAgoraUid().toString()
        )
        observe()
        connectingTimer.start()
    }

    private fun startDisconnectListener() {
        disconnectListenerJob = scope.launch {
            loop@ while (true) {
                try {
                    ensureActive()
                    val event = context.getStreamPipe().receive()
                    Log.d(TAG, "Received after observing : ${event.type}")
                    ensureActive()
                    if (event.type == CALL_DISCONNECTED) {
                        context.isRetrying = true
                        PrefManager.setVoipState(State.SEARCHING)
                        context.state = SearchingState(context)
                        scope.cancel()
                    } else {
                        ensureActive()
                        val msg = "In $TAG but received ${event.type} expected $CALL_DISCONNECTED"
                        CallAnalytics.addAnalytics(
                            event = EventName.ILLEGAL_EVENT_RECEIVED,
                            agoraCallId = context.channelData.getCallingId().toString(),
                            agoraMentorId = context.channelData.getAgoraUid().toString(),
                            extra = msg
                        )
                        throw IllegalEventException(msg)
                    }
                } catch (e: Throwable) {
                    if (e is CancellationException)
                        throw e
                    if (e is IllegalEventException) {
                        e.printStackTrace()
                    } else {
                        e.printStackTrace()
                        PrefManager.setVoipState(State.IDLE)
                        Log.d(TAG, "EXCEPTION : $e switched to IDLE STATE")
                        context.closeCallScreen()
                        context.closePipe()
                        onDestroy()
                    }
                }
            }
        }
    }

    // Red Button Pressed
    override fun disconnect() {
        scope.launch {
            try{
                Log.d(TAG, "disconnect : User Red Press switching to Leaving State")
                connectingTimer.cancel()
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
        CallAnalytics.addAnalytics(
            event = EventName.BACK_PRESSED,
            agoraCallId = context.channelData.getCallingId().toString(),
            agoraMentorId = context.channelData.getAgoraUid().toString(),
            extra = TAG
        )
        // TODO: Will Remove
        if(context.channelData.isNewSearchingEnabled())
            disconnect()
    }

    override fun onError(reason: String) {
        CallAnalytics.addAnalytics(
            event = EventName.ON_ERROR,
            agoraCallId = context.channelData.getCallingId().toString(),
            agoraMentorId = context.channelData.getAgoraUid().toString(),
            extra = "In $TAG : $reason"
        )
        connectingTimer.cancel()
        disconnect()
    }

    override fun onDestroy() {
        connectingTimer.cancel()
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
                            connectingTimer.cancel()
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
                            context.enableSpeaker(true)
                            context.updateUIState(uiState = uiState)
                        }
                        SPEAKER_OFF_REQUEST -> {
                            val uiState = context.currentUiState.copy(isSpeakerOn = false)
                            context.enableSpeaker(false)
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
                        REMOTE_USER_DISCONNECTED_MESSAGE, REMOTE_USER_DISCONNECTED_AGORA, REMOTE_USER_DISCONNECTED_USER_LEFT -> {
                            // Ignore Error Event from Agora
                            val msg = "Ignoring : In $TAG but received ${event.type} expected $CALL_CONNECTED_EVENT"
                            CallAnalytics.addAnalytics(
                                event = EventName.ILLEGAL_EVENT_RECEIVED,
                                agoraCallId = context.channelData.getCallingId().toString(),
                                agoraMentorId = context.channelData.getAgoraUid().toString(),
                                extra = msg
                            )
                            Log.d(TAG, "Ignoring : In $TAG but received ${event.type} expected $CALL_CONNECTED_EVENT")
                        }
                        RECONNECTED, RECEIVED_CHANNEL_DATA, RECONNECTING -> {
                            // Ignore Error Event from Agora
                            val msg = "Ignoring : In $TAG but received ${event.type} expected $CALL_CONNECTED_EVENT"
                            CallAnalytics.addAnalytics(
                                event = EventName.ILLEGAL_EVENT_RECEIVED,
                                agoraCallId = context.channelData.getCallingId().toString(),
                                agoraMentorId = context.channelData.getAgoraUid().toString(),
                                extra = msg
                            )
                            Log.d(TAG, "Ignoring : In $TAG but received ${event.type} expected $CALL_CONNECTED_EVENT")
                        }
                        Event.INTEREST -> {
                            val uiData = event.data as Interest
                            val uiState = context.currentUiState.copy(
                                interestHeader = uiData.getInterestHeader(),
                                interests = uiData.getInterests(),
                            )
                            context.updateUIState(uiState = uiState)
                        }
                        else -> {
                            val msg = "In $TAG but received ${event.type} expected $CALL_CONNECTED_EVENT"
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
        scope.launch {
            try {
                context.disconnectCall()
                PrefManager.setVoipState(State.LEAVING)
                context.state = LeavingState(context)
                Log.d(TAG, "Received : switched to ${context.state}")
                scope.cancel()
            } catch (e : Exception){
                if(e is CancellationException)
                    throw e
                e.printStackTrace()
            }
        }
    }
}

data class CallConnectData(val startTime: Long, val userName: String)