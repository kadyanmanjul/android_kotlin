package com.joshtalks.joshskills.voip.communication

import android.util.Log
import com.google.gson.Gson
import com.joshtalks.joshskills.voip.Utils
import com.joshtalks.joshskills.voip.communication.constants.ServerConstants.Companion.ACK_UI_STATE_UPDATED
import com.joshtalks.joshskills.voip.communication.constants.ServerConstants.Companion.CHANNEL
import com.joshtalks.joshskills.voip.communication.constants.ServerConstants.Companion.GROUP_INCOMING_CALL
import com.joshtalks.joshskills.voip.communication.constants.ServerConstants.Companion.INCOMING_CALL
import com.joshtalks.joshskills.voip.communication.constants.ServerConstants.Companion.UI_STATE_UPDATED
import com.joshtalks.joshskills.voip.communication.model.*
import com.joshtalks.joshskills.voip.data.local.PrefManager
import com.joshtalks.joshskills.voip.voipLog
import com.pubnub.api.PubNub
import com.pubnub.api.callbacks.SubscribeCallback
import com.pubnub.api.enums.PNStatusCategory
import com.pubnub.api.enums.PNStatusCategory.*
import com.pubnub.api.models.consumer.PNStatus
import com.pubnub.api.models.consumer.objects_api.channel.PNChannelMetadataResult
import com.pubnub.api.models.consumer.objects_api.membership.PNMembershipResult
import com.pubnub.api.models.consumer.objects_api.uuid.PNUUIDMetadataResult
import com.pubnub.api.models.consumer.pubsub.PNMessageResult
import com.pubnub.api.models.consumer.pubsub.PNPresenceEventResult
import com.pubnub.api.models.consumer.pubsub.PNSignalResult
import com.pubnub.api.models.consumer.pubsub.files.PNFileEventResult
import com.pubnub.api.models.consumer.pubsub.message_actions.PNMessageActionResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch

private const val TAG = "PubNubSubscriber"
internal class PubNubSubscriber(val scope: CoroutineScope) : SubscribeCallback() {

    private val messageFlow by lazy<MutableSharedFlow<Communication>> {
        MutableSharedFlow(replay = 0)
    }

    private val stateFlow by lazy<MutableSharedFlow<PubnubState>> {
        MutableSharedFlow(replay = 0)
    }

    fun observeMessages() : SharedFlow<Communication> {
        return messageFlow
    }

    fun observeChannelState() : SharedFlow<PubnubState> {
        return stateFlow
    }
// 19897969509
    override fun message(pubnub: PubNub, pnMessageResult: PNMessageResult) {
        scope.launch {
            try {
                val messageJson = pnMessageResult.message.asJsonObject.apply {
                    addProperty("timetoken", pnMessageResult.timetoken)
                }
                // So that we will ignore our own message
                if(pnMessageResult.publisher == Utils.uuid)
                    return@launch
                Log.d(TAG, "Raw message : $pnMessageResult")
                val message = when(pnMessageResult.userMetadata.asInt) {
                    CHANNEL -> Gson().fromJson(messageJson, Channel::class.java)
                    INCOMING_CALL -> Gson().fromJson(messageJson, IncomingCall::class.java)
                    GROUP_INCOMING_CALL -> Gson().fromJson(messageJson, GroupIncomingCall::class.java)
                    UI_STATE_UPDATED, ACK_UI_STATE_UPDATED -> Gson().fromJson(messageJson, UI::class.java)
                    else -> Gson().fromJson(messageJson, Message::class.java)
                }
                messageFlow.emit(message)
            } catch (e : Exception) {
                e.printStackTrace()
                messageFlow.emit(Error())
                if(e is CancellationException)
                    throw e
            }
        }
    }

    override fun status(pubnub: PubNub, status: PNStatus) {
        //Log.d(TAG, "status: Status --> ${status}")
        when(status.category) {
            PNConnectedCategory -> {
                try{
                    scope.launch {
                        val lastMessageTime = PrefManager.getLatestPubnubMessageTime()
                        //Log.d(TAG, "status: Last Msg Time -> $lastMessageTime")
                        if(lastMessageTime == 0L) {
                            PrefManager.setLatestPubnubMessageTime(pubnub.time().sync()?.timetoken ?: 0)
                            val afterUpdate = PrefManager.getLatestPubnubMessageTime()
                            //Log.d(TAG, "status: After Update --> $afterUpdate")
                        }
                    }
                }
                catch (e : Exception){
                    if(e is CancellationException)
                        throw e
                    e.printStackTrace()
                }
            }
            PNReconnectedCategory -> {
                sendEvent(PubnubState.RECONNECTED)
            }
            PNUnexpectedDisconnectCategory -> {
                // internet got lost
                //Log.d(TAG, "status: PNUnexpectedDisconnectCategory")
                pubnub.reconnect();
            }
            PNTimeoutCategory -> {
                //reconnect when ready
                //Log.d(TAG, "status: PNTimeoutCategory")
                pubnub.reconnect();
            }
        }
    }

    private fun sendEvent(event : PubnubState) {
        scope.launch {
            try {
                stateFlow.emit(event)
            } catch (e : Exception) {
                if(e is CancellationException)
                    throw e
                else
                    e.printStackTrace()
            }
        }
    }

    override fun presence(pubnub: PubNub, pnPresenceEventResult: PNPresenceEventResult) {}

    override fun signal(pubnub: PubNub, pnSignalResult: PNSignalResult) {}

    override fun uuid(pubnub: PubNub, pnUUIDMetadataResult: PNUUIDMetadataResult) {}

    override fun channel(pubnub: PubNub, pnChannelMetadataResult: PNChannelMetadataResult) {}

    override fun membership(pubnub: PubNub, pnMembershipResult: PNMembershipResult) {}

    override fun messageAction(pubnub: PubNub, pnMessageActionResult: PNMessageActionResult) {}

    override fun file(pubnub: PubNub, pnFileEventResult: PNFileEventResult) {}
}