package com.joshtalks.joshskills.voip.state

import android.util.Log
import com.joshtalks.joshskills.voip.Utils
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
            try{
                ensureActive()
                val timeout = Timeout(ServerConstants.TIMEOUT)
                for (i in 1..12) {
                    delay(PER_USER_TIMEOUT_IN_MILLIS)
                    ensureActive()
                    context.sendMessageToServer(timeout)
                }
                delay(PER_USER_TIMEOUT_IN_MILLIS)
                ensureActive()
                disconnectNoUserFound()
                cleanUpState()
            }
            catch (e : Exception){
                if(e is CancellationException)
                    throw e
                e.printStackTrace()
            }
        }
    }
    private val calling by lazy<Calling> { PeerToPeerCalling() }

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
        scope.launch {
            try {
                calling.onPreCallConnect(context.request, context.direction)
            } catch (e: Exception) {
                if (e is HttpException || e is SocketTimeoutException)
                    Log.d(TAG, " Exception : API failed")
                e.printStackTrace()
                ensureActive()
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

    override fun onError() {
        scope.launch {
            try{
                context.closeCallScreen()
                backPress()
            }
            catch (e : Exception){
                if(e is CancellationException)
                    throw e
                e.printStackTrace()
            }

        }
    }

    private fun observe() {
        Log.d(TAG, "Started Observing")
        scope.launch {
            try {
                val event = context.getStreamPipe().receive()
                Log.d(TAG, "Received after observing : ${event.type}")
                if (event.type == RECEIVED_CHANNEL_DATA) {
                    Log.d(TAG, "observe: Received Channel Data")
                    context.channelData = event.data as? ChannelData
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
                    PrefManager.setVoipState(State.JOINING)
                    context.state = JoiningState(context)
                    Log.d(TAG, "Received : ${event.type} switched to ${context.state}")
                } else
                    throw IllegalEventException("In $TAG but received ${event.type} expected $RECEIVED_CHANNEL_DATA")
                scope.cancel()
            } catch (e: Throwable) {
                if(e is CancellationException)
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
            try{
                context.closeCallScreen()
                context.destroyContext()
            }
            catch (e : Exception){
                if(e is CancellationException)
                    throw e
                e.printStackTrace()
            }

        }
    }
}

