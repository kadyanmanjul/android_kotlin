package com.joshtalks.joshskills.voip.state

import android.util.Log
import com.joshtalks.joshskills.voip.Utils
import com.joshtalks.joshskills.voip.communication.constants.ServerConstants
import com.joshtalks.joshskills.voip.communication.model.ChannelData
import com.joshtalks.joshskills.voip.communication.model.NetworkAction
import com.joshtalks.joshskills.voip.communication.model.Timeout
import com.joshtalks.joshskills.voip.communication.model.UserAction
import com.joshtalks.joshskills.voip.constant.RECEIVED_CHANNEL_DATA
import com.joshtalks.joshskills.voip.data.UIState
import com.joshtalks.joshskills.voip.data.local.PrefManager
import com.joshtalks.joshskills.voip.mediator.CallDirection
import com.joshtalks.joshskills.voip.mediator.Calling
import com.joshtalks.joshskills.voip.mediator.PER_USER_TIMEOUT_IN_MILLIS
import com.joshtalks.joshskills.voip.mediator.PeerToPeerCalling
import kotlinx.coroutines.*
import retrofit2.HttpException
import java.net.SocketTimeoutException

// Fired an API So We have to make sure how to cancel
/**
 * 1. Call API
 * 2. Stay in this state
 * 3. Once receive Channel Switch to Joining State
 */
class SearchingState(val context: CallContext) : VoipState {
    private val TAG = "SearchingState"
    private val scope = CoroutineScope(Dispatchers.IO)
    private val timeoutTimer by lazy {
        scope.launch(start = CoroutineStart.LAZY) {
            ensureActive()
            val timeout = Timeout(ServerConstants.TIMEOUT)
            for (i in 1..12) {
                delay(PER_USER_TIMEOUT_IN_MILLIS)
                ensureActive()
                context.sendMessageToServer(timeout)
                Log.d(TAG, "startUserNotFoundTimer: $i")
            }
            Log.d(TAG, "startUserNotFoundTimer: Sending Disconnect")
            delay(PER_USER_TIMEOUT_IN_MILLIS)
            ensureActive()
            disconnectNoUserFound()
            Log.d(TAG, "startUserNotFoundTimer: disconnectCall()")
            context.disconnectCall()
            Log.d(TAG, "startUserNotFoundTimer: CALL_DISCONNECT_REQUEST")
            scope.launch { context.closeCallScreen() }
        }
    }
    private val calling by lazy<Calling> { PeerToPeerCalling() }

    init {
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
        scope.launch {
            try {
                calling.onPreCallConnect(context.request, context.direction)
            } catch (e: Exception) {
                if (e is HttpException || e is SocketTimeoutException)
                    Log.d(TAG, " Exception : API failed")
                e.printStackTrace()
                context.destroyContext()
                // TODO : Emit Error - Close User Screen
            }
        }
    }

    // TODO: What will happen if user pickup the call and press back button
    override fun backPress() {
        Log.d(TAG, "backPress: ")
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
        context.destroyContext()
    }

    private fun observe() {
        scope.launch {
            try {
                val event = context.getStreamPipe().receive()
                if (event.what == RECEIVED_CHANNEL_DATA) {
                    Log.d(TAG, "observe: Received Channel Data")
                    context.channelData = event.obj as? ChannelData
                        ?: throw UnexpectedException("Channel data is NULL")
                    PrefManager.setLocalUserAgoraId(context.channelData.getAgoraUid())
                    val uiState = UIState(
                        remoteUserImage = context.channelData.getCallingPartnerImage(),
                        remoteUserName = context.channelData.getCallingPartnerName(),
                        callType = context.channelData.getType(),
                        topicName = context.channelData.getCallingTopic()
                    )
                    context.updateUIState(uiState)
                    if (context.direction == CallDirection.OUTGOING)
                        timeoutTimer.cancel()
                    ensureActive()
                    context.state = JoiningState(context)
                } else
                    throw IllegalEventException("In $TAG but received ${event.what} expected $RECEIVED_CHANNEL_DATA")
                scope.cancel()
            } catch (e: Exception) {
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
        scope.cancel()
    }

    private fun cleanUpState() {
        scope.launch {
            context.closeCallScreen()
            context.destroyContext()
        }
    }
}

