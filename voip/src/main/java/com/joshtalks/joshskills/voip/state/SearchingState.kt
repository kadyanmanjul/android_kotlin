package com.joshtalks.joshskills.voip.state

import android.util.Log
import com.joshtalks.joshskills.base.constants.INTENT_DATA_PREVIOUS_CALL_ID
import com.joshtalks.joshskills.voip.Utils
import com.joshtalks.joshskills.voip.Utils.Companion.ignoreException
import com.joshtalks.joshskills.voip.communication.constants.ServerConstants
import com.joshtalks.joshskills.voip.communication.model.ChannelData
import com.joshtalks.joshskills.voip.communication.model.NetworkAction
import com.joshtalks.joshskills.voip.communication.model.Timeout
import com.joshtalks.joshskills.voip.communication.model.UserAction
import com.joshtalks.joshskills.voip.constant.JOINING
import com.joshtalks.joshskills.voip.constant.Event.*
import com.joshtalks.joshskills.voip.constant.State
import com.joshtalks.joshskills.voip.data.UIState
import com.joshtalks.joshskills.voip.data.local.PrefManager
import com.joshtalks.joshskills.voip.mediator.CallDirection
import com.joshtalks.joshskills.voip.mediator.Calling
import com.joshtalks.joshskills.voip.mediator.PER_USER_TIMEOUT_IN_MILLIS
import com.joshtalks.joshskills.voip.mediator.PeerToPeerCalling
import com.joshtalks.joshskills.voip.voipanalytics.CallAnalytics
import com.joshtalks.joshskills.voip.voipanalytics.EventName
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import okhttp3.internal.ignoreIoExceptions
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException

// Fired an API So We have to make sure how to cancel
/**
 * 1. Call API
 * 2. Stay in this state
 * 3. Once receive Channel Switch to Joining State
 */
class SearchingState(val context: CallContext) : VoipState {
    private val TAG = "SearchingState"
    private val scope =
        CoroutineScope(Dispatchers.IO + CoroutineExceptionHandler { coroutineContext, throwable ->
            Log.d(TAG, "CoroutineExceptionHandler : $throwable")
            throwable.printStackTrace()
        })
    private var listenerJob: Job? = null
    private val timeoutTimer by lazy {
        scope.launch(start = CoroutineStart.LAZY) {
            try {
                ensureActive()
                val timeout = Timeout(ServerConstants.TIMEOUT)
                for (i in 1..12) {
                    delay(PER_USER_TIMEOUT_IN_MILLIS)
                    ensureActive()
                    ignoreException { context.sendMessageToServer(timeout) }
                }
                delay(PER_USER_TIMEOUT_IN_MILLIS)
                disconnectNoUserFound()
                cleanUpState()
            } catch (e: Exception) {
                if (e is CancellationException)
                    throw e
                e.printStackTrace()
            }
        }
    }
    private val calling by lazy<Calling> { PeerToPeerCalling() }
    private val apiCallJob by lazy {
        scope.launch(start = CoroutineStart.LAZY) {
            try {
                ensureActive()
                CallAnalytics.addAnalytics(
                    event = EventName.CALL_INITIATED,
                    agoraCallId = "",
                    agoraMentorId = PrefManager.getLocalUserAgoraId().toString()
                )
                if (context.isRetrying) {
                    context.request[INTENT_DATA_PREVIOUS_CALL_ID] =
                        context.channelData.getCallingId()
                    CallAnalytics.addAnalytics(
                        event = EventName.NEXT_CHANNEL_REQUESTED,
                        agoraCallId = context.channelData.getCallingId().toString(),
                        agoraMentorId = context.channelData.getAgoraUid().toString(),
                        extra = TAG
                    )
                }
                calling.onPreCallConnect(context.request, context.direction)
                ensureActive()
            } catch (e: Exception) {
                e.printStackTrace()
                when (e) {
                    is HttpException, is SocketTimeoutException -> {
                        Log.d(TAG, " Exception : API failed")
                        e.printStackTrace()
                        ensureActive()
                        disconnectNoUserFound()
                        cleanUpState()
                    }
                    is CancellationException -> {
                        throw e
                    }
                    else -> {
                        ensureActive()
                        disconnectNoUserFound()
                        cleanUpState()
                    }
                }
            }
        }
    }

    init {
        Log.d("Call State", TAG)
        observe()
        /* 1. Setting Call type
         -------- Applicable for Incoming Call --------
         2. Checking Notification
         3. Removing Notification
         4. Stopping Ringtone Audio
         -----------
         5. Cancel timeout timer  (Just to make sure if last timer didn't canceled)
         6. Checking is Incoming Call
         7. If its outgoing Call then start timeout timer*/
        if (context.direction == CallDirection.OUTGOING)
            timeoutTimer.start()
        apiCallJob.start()
    }

    override fun disconnect() {
        scope.launch {
            context.closeCallScreen()
            sendDataToServer()
        }
    }

    // TODO: What will happen if user pickup the call and press back button
    override fun backPress() {
        Log.d(TAG, "backPress: ")
        CallAnalytics.addAnalytics(
            event = EventName.BACK_PRESSED,
            agoraCallId = "-1",
            agoraMentorId = "-1",
            extra = TAG
        )
        CallAnalytics.addAnalytics(
            event = EventName.DISCONNECTED_BY_BACKPRESS,
            agoraCallId = "-1",
            agoraMentorId = "-1",
            extra = TAG
        )
        sendDataToServer()
    }

    override fun onError() {
        CallAnalytics.addAnalytics(
            event = EventName.ON_ERROR,
            agoraMentorId = "-1",
            extra = TAG
        )
        scope.launch {
            try {
                context.closeCallScreen()
                sendDataToServer()
            } catch (e: Exception) {
                if (e is CancellationException)
                    throw e
                e.printStackTrace()
            }

        }
    }

    private fun sendDataToServer() {
        if (context.direction == CallDirection.OUTGOING)
            timeoutTimer.cancel()
        val networkAction = NetworkAction(
            channelName = "",
            uid = 0,
            type = ServerConstants.DISCONNECTED,
            duration = 0,
            address = Utils.uuid ?: ""
        )
        context.sendMessageToServer(networkAction)
        context.closePipe()
        onDestroy()
    }

    private fun observe() {
        Log.d(TAG, "Started Observing")
        listenerJob = scope.launch {
            try {
                loop@ while (true) {
                    val event = context.getStreamPipe().receive()
                    Log.d(TAG, "Received after observing : ${event.type}")
                    when (event.type) {
                        RECEIVED_CHANNEL_DATA -> {
                            Log.d(TAG, "observe: Received Channel Data")
                            apiCallJob.cancel()
                            context.channelData = event.data as? ChannelData
                                ?: throw UnexpectedException("Channel data is NULL")
                            CallAnalytics.addAnalytics(
                                event = EventName.CHANNEL_RECEIVED,
                                agoraCallId = context.channelData.getCallingId().toString(),
                                agoraMentorId = context.channelData.getAgoraUid().toString()
                            )
                            PrefManager.setLocalUserAgoraIdAndCallId(
                                context.channelData.getAgoraUid(),
                                context.channelData.getCallingId()
                            )
                            val uiState = UIState(
                                remoteUserImage = context.channelData.getCallingPartnerImage(),
                                remoteUserName = context.channelData.getCallingPartnerName(),
                                callType = context.channelData.getType(),
                                topicName = context.channelData.getCallingTopic(),
                                currentTopicImage = context.channelData.getTopicImage(),
                                occupation = context.channelData.getOccupation(),
                                aspiration = context.channelData.getAspiration()
                            )
                            context.updateUIState(uiState)
                            if (context.direction == CallDirection.OUTGOING)
                                timeoutTimer.cancel()
                            ensureActive()
                            PrefManager.setVoipState(State.JOINING)
                            context.state = JoiningState(context)
                            Log.d(TAG, "Received : ${event.type} switched to ${context.state}")
                            break@loop
                        }
                        TOPIC_IMAGE_RECEIVED, SYNC_UI_STATE, REMOTE_USER_DISCONNECTED_MESSAGE, REMOTE_USER_DISCONNECTED_AGORA, REMOTE_USER_DISCONNECTED_USER_LEFT -> {
                            val msg =
                                "Ignoring : In $TAG but received ${event.type} expected $RECEIVED_CHANNEL_DATA"
                            CallAnalytics.addAnalytics(
                                event = EventName.ILLEGAL_EVENT_RECEIVED,
                                agoraCallId = context.channelData.getCallingId().toString(),
                                agoraMentorId = context.channelData.getAgoraUid().toString(),
                                extra = msg
                            )
                            Log.d(
                                TAG,
                                "Ignoring : In $TAG but received ${event.type} expected $RECEIVED_CHANNEL_DATA"
                            )
                        }
                        else -> {
                            val msg =
                                "In $TAG but received ${event.type} expected $RECEIVED_CHANNEL_DATA"
                            CallAnalytics.addAnalytics(
                                event = EventName.ILLEGAL_EVENT_RECEIVED,
                                agoraCallId = "-1",
                                agoraMentorId = "-1",
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
                e.printStackTrace()
                cleanUpState()
            }
        }
    }

    private fun disconnectNoUserFound() {
        val noUserFound = UserAction(
            channelName = "NO_CHANNEL",
            type = ServerConstants.NO_USER_FOUND,
            address = Utils.uuid ?: ""
        )
        context.sendMessageToServer(noUserFound)
    }

    override fun onDestroy() {
        PrefManager.setVoipState(State.IDLE)
        scope.cancel()
    }

    private fun cleanUpState() {
        scope.launch {
            try {
                context.closeCallScreen()
                context.closeCallScreen()
                context.closePipe()
                onDestroy()
            } catch (e: Exception) {
                if (e is CancellationException)
                    throw e
                e.printStackTrace()
            }

        }
    }
}