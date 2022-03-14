package com.joshtalks.joshskills.ui.group.lib

import android.os.Message
import com.google.gson.Gson
import com.joshtalks.joshskills.base.EventLiveData
import com.joshtalks.joshskills.constants.REMOVE_GROUP_AND_CLOSE
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.ui.group.FROM_BACKEND_MSG_TIME
import com.joshtalks.joshskills.ui.group.constants.RECEIVE_META_MESSAGE_LOCAL
import com.joshtalks.joshskills.ui.group.constants.SENT_META_MESSAGE_LOCAL
import com.joshtalks.joshskills.ui.group.model.ChatItem
import com.joshtalks.joshskills.ui.group.model.MessageItem
import com.joshtalks.joshskills.ui.group.utils.getLastMessage
import com.joshtalks.joshskills.ui.group.utils.getMessageType
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object ChatSubscriber : SubscribeCallback() {
    private val database = AppObjectController.appDatabase

    override fun status(pubnub: PubNub, pnStatus: PNStatus) {}

    override fun message(pubnub: PubNub, pnMessageResult: PNMessageResult) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val messageItem = Gson().fromJson(pnMessageResult.message, MessageItem::class.java)
                if (pnMessageResult.userMetadata.asString != FROM_BACKEND_MSG_TIME)
                    database.groupListDao().updateGroupItem(
                        lastMessage = messageItem.getLastMessage(pnMessageResult.userMetadata.asString, messageItem.msgType),
                        lastMsgTime = pnMessageResult.timetoken,
                        id = pnMessageResult.channel
                    )
                // Meta + Sender
                database.groupChatDao().insertMessage(
                    ChatItem(
                        sender = pnMessageResult.userMetadata.asString,
                        message = messageItem.msg,
                        msgTime = pnMessageResult.timetoken,
                        groupId = pnMessageResult.channel,
                        msgType = messageItem.getMessageType(),
                        messageId = "${pnMessageResult.timetoken}_${pnMessageResult.channel}_${messageItem.mentorId}"
                    )
                )
                val message = messageItem.msg
                if (messageItem.getMessageType() == RECEIVE_META_MESSAGE_LOCAL && message.contains("changed")) {
                    when (message.contains("changed the group icon")) {
                        true -> {
                            //TODO("UPDATE IMAGE ICON")
                        }
                        false -> {
                            val newGroupName = message.substring(message.lastIndexOf("the group name to ") + 18)
                            database.groupListDao().updateGroupName(pnMessageResult.channel, newGroupName)
                        }
                    }
                } else if (messageItem.getMessageType() == SENT_META_MESSAGE_LOCAL && message.contains("removed")) {
                    if (messageItem.mentorId == Mentor.getInstance().getId()) {
                        withContext(Dispatchers.Main) {
                            val messageObj = Message()
                            messageObj.what = REMOVE_GROUP_AND_CLOSE
                            messageObj.obj = pnMessageResult.channel
                            EventLiveData.value = messageObj
                        }
                    }
                }
            } catch (e : Exception) {
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