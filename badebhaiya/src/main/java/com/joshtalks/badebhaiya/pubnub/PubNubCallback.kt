package com.joshtalks.badebhaiya.pubnub

import android.util.Log
import com.joshtalks.badebhaiya.core.LogException
import com.joshtalks.badebhaiya.liveroom.adapter.PubNubEvent
import com.joshtalks.badebhaiya.liveroom.model.ConversationRoomPubNubEventBus
import com.pubnub.api.PubNub
import com.pubnub.api.callbacks.SubscribeCallback
import com.pubnub.api.models.consumer.PNStatus
import com.pubnub.api.models.consumer.objects_api.channel.PNChannelMetadataResult
import com.pubnub.api.models.consumer.objects_api.membership.PNMembershipResult
import com.pubnub.api.models.consumer.objects_api.uuid.PNUUIDMetadataResult
import com.pubnub.api.models.consumer.pubsub.PNMessageResult
import com.pubnub.api.models.consumer.pubsub.PNPresenceEventResult
import com.pubnub.api.models.consumer.pubsub.PNSignalResult
import com.pubnub.api.models.consumer.pubsub.files.PNFileEventResult
import com.pubnub.api.models.consumer.pubsub.message_actions.PNMessageActionResult
import timber.log.Timber

/**
    This object handles all the callbacks from PubNub.
*/

class PubNubCallback: SubscribeCallback() {

    override fun status(pubnub: PubNub, pnStatus: PNStatus) {
    }

    override fun message(pubnub: PubNub, pnMessageResult: PNMessageResult) {
        val msg = pnMessageResult.message.asJsonObject
        val act = msg["action"].asString
        try {
            if (msg != null) {
                Log.d(
                    "ABCEvent",
                    "message() called with: pubnub = $pubnub, pnMessageResult = $pnMessageResult"
                )
//                PubNubManager.eventExists()
                Timber.d("ABC Event ka msg hai => $msg and PN message result is => $pnMessageResult")
//                PubNubData.eventsMap[pnMessageResult.timetoken] = msg
                PubNubManager.postToPubNubEvent(
                    ConversationRoomPubNubEventBus(
                        PubNubEvent.valueOf(act),
                        msg,
                        pnMessageResult.timetoken
                    )
                )
            }
        } catch (ex: Exception) {
            LogException.catchException(ex)
        }
    }

    override fun presence(pubnub: PubNub, pnPresenceEventResult: PNPresenceEventResult) {
    }

    override fun signal(pubnub: PubNub, pnSignalResult: PNSignalResult) {
    }

    override fun uuid(pubnub: PubNub, pnUUIDMetadataResult: PNUUIDMetadataResult) {
    }

    override fun channel(pubnub: PubNub, pnChannelMetadataResult: PNChannelMetadataResult) {
    }

    override fun membership(pubnub: PubNub, pnMembershipResult: PNMembershipResult) {
    }

    override fun messageAction(pubnub: PubNub, pnMessageActionResult: PNMessageActionResult) {
    }

    override fun file(pubnub: PubNub, pnFileEventResult: PNFileEventResult) {
    }
}