package com.joshtalks.joshskills.voip.communication

import android.util.Log
import com.google.gson.Gson
import com.joshtalks.joshskills.voip.communication.constants.ServerConstants.Companion.CHANNEL
import com.joshtalks.joshskills.voip.communication.constants.ServerConstants.Companion.INCOMING_CALL
import com.joshtalks.joshskills.voip.communication.model.Channel
import com.joshtalks.joshskills.voip.communication.model.Communication
import com.joshtalks.joshskills.voip.communication.model.Error
import com.joshtalks.joshskills.voip.communication.model.IncomingCall
import com.joshtalks.joshskills.voip.communication.model.Message
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch

private const val TAG = "PubNubSubscriber"
internal class PubNubSubscriber(val scope: CoroutineScope) : SubscribeCallback() {

    private val messageFlow by lazy<MutableSharedFlow<Communication>> {
        Log.d(TAG, "Creating : messageFlow")
        MutableSharedFlow(replay = 0)
    }

    fun observeMessages() : SharedFlow<Communication> {
        return messageFlow
    }

    override fun message(pubnub: PubNub, pnMessageResult: PNMessageResult) {
        scope.launch {
            Log.d(TAG, "message: $pnMessageResult")
            val messageJson = pnMessageResult.message.asJsonObject.apply {
                addProperty("timetoken", pnMessageResult.timetoken)
            }
            Log.d(TAG, "message with Time : $messageJson")
            try {
                // So that we will ignore our own message
                if(pnMessageResult.userMetadata == null)
                    return@launch
                val message = when(pnMessageResult.userMetadata.asInt) {
                    CHANNEL -> Gson().fromJson(messageJson, Channel::class.java)
                    INCOMING_CALL -> Gson().fromJson(messageJson, IncomingCall::class.java)
                    else -> Gson().fromJson(messageJson, Message::class.java)
                }
                messageFlow.emit(message)
            } catch (e : Exception) {
                e.printStackTrace()
                messageFlow.emit(Error())
            }
        }
    }

    override fun status(pubnub: PubNub, status: PNStatus) {
        //Log.d(TAG, "status: Category --> ${status.category}")
        //Log.d(TAG, "status: Status --> ${status}")
        when(status.category) {
            PNConnectedCategory -> {
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

    override fun presence(pubnub: PubNub, pnPresenceEventResult: PNPresenceEventResult) {}

    override fun signal(pubnub: PubNub, pnSignalResult: PNSignalResult) {}

    override fun uuid(pubnub: PubNub, pnUUIDMetadataResult: PNUUIDMetadataResult) {}

    override fun channel(pubnub: PubNub, pnChannelMetadataResult: PNChannelMetadataResult) {}

    override fun membership(pubnub: PubNub, pnMembershipResult: PNMembershipResult) {}

    override fun messageAction(pubnub: PubNub, pnMessageActionResult: PNMessageActionResult) {}

    override fun file(pubnub: PubNub, pnFileEventResult: PNFileEventResult) {}
}