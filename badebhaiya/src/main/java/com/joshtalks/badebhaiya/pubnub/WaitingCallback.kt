package com.joshtalks.badebhaiya.pubnub

import android.os.Message
import android.util.Log
import com.joshtalks.badebhaiya.core.LogException
import com.joshtalks.badebhaiya.liveroom.JOINED
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

/**
    This object handles all the callbacks from PubNub.
*/

class WaitingCallback: SubscribeCallback() {

    override fun status(pubnub: PubNub, pnStatus: PNStatus) {
    }

    override fun message(pubnub: PubNub, pnMessageResult: PNMessageResult) {
        val msg = pnMessageResult.message.asJsonObject
        //val act = msg["action"].asString
        try {
            if (msg != null) {
                Log.d(
                    "ABCEvent",
                    "message() called with: pubnub = $pubnub, pnMessageResult = $pnMessageResult"
                )
                var mess=Message()
                mess.what=JOINED
                PubNubManager.postToSpeakerStatus(
                    mess
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