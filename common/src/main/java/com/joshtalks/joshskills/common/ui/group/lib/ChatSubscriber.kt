package com.joshtalks.joshskills.common.ui.group.lib

import android.os.Message
import com.google.gson.Gson
import com.joshtalks.joshskills.common.base.EventLiveData
import com.joshtalks.joshskills.common.constants.REMOVE_AND_BLOCK_FPP
import com.joshtalks.joshskills.common.constants.REMOVE_GROUP_AND_CLOSE
import com.joshtalks.joshskills.common.core.AppObjectController
import com.joshtalks.joshskills.common.repository.local.model.Mentor
import com.joshtalks.joshskills.common.ui.group.constants.FROM_BACKEND_MSG_TIME
import com.joshtalks.joshskills.common.ui.group.constants.RECEIVE_META_MESSAGE_LOCAL
import com.joshtalks.joshskills.common.ui.group.constants.SENT_META_MESSAGE_LOCAL
import com.joshtalks.joshskills.common.ui.group.model.ChatItem
import com.joshtalks.joshskills.common.ui.group.model.GroupMember
import com.joshtalks.joshskills.common.ui.group.model.MessageItem
import com.joshtalks.joshskills.common.ui.group.utils.getLastMessage
import com.joshtalks.joshskills.common.ui.group.utils.getMessageType
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

    private const val TAG = "ChatSubscriber"
    private val database = AppObjectController.appDatabase
    private val chatService: ChatService = PubNubService

    override fun status(pubnub: PubNub, pnStatus: PNStatus) {}

    override fun message(pubnub: PubNub, pnMessageResult: PNMessageResult) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val messageItem = Gson().fromJson(pnMessageResult.message, MessageItem::class.java)
                database.groupListDao().updateGroupItem(
                    lastMessage = messageItem.getLastMessage(pnMessageResult.userMetadata.asString, messageItem.msgType),
                    lastMsgTime = pnMessageResult.timetoken,
                    id = pnMessageResult.channel
                )
                var messageTime = pnMessageResult.timetoken
                if (pnMessageResult.userMetadata.asString == FROM_BACKEND_MSG_TIME)
                    messageTime = messageItem.msg.toLong().times(10000000)
                // Meta + Sender
                database.groupChatDao().insertMessage(
                    ChatItem(
                        sender = pnMessageResult.userMetadata.asString,
                        message = messageItem.msg,
                        msgTime = messageTime,
                        groupId = pnMessageResult.channel,
                        msgType = messageItem.getMessageType(),
                        messageId = "${messageTime}_${pnMessageResult.channel}_${messageItem.mentorId}"
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
                            messageObj.what =
                                com.joshtalks.joshskills.common.constants.REMOVE_GROUP_AND_CLOSE
                            messageObj.obj = pnMessageResult.channel
                            com.joshtalks.joshskills.common.base.EventLiveData.value = messageObj
                        }
                    }
                } else if (messageItem.getMessageType() == SENT_META_MESSAGE_LOCAL && message.contains("block")) {
                    if (messageItem.mentorId == Mentor.getInstance().getId()) {
                        withContext(Dispatchers.Main) {
                            val messageObj = Message()
                            messageObj.what =
                                com.joshtalks.joshskills.common.constants.REMOVE_AND_BLOCK_FPP
                            messageObj.obj = pnMessageResult.channel
                            com.joshtalks.joshskills.common.base.EventLiveData.value = messageObj
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

    override fun membership(pubnub: PubNub, pnMembershipResult: PNMembershipResult) {
        CoroutineScope(Dispatchers.IO).launch {
            val event = pnMembershipResult.event
            val groupId = pnMembershipResult.channel
            val mentorId = pnMembershipResult.data.uuid

            if (event == "set") {
                database.groupMemberDao().insertMember(GroupMember(
                    mentorID = mentorId,
                    memberName = chatService.getUserMetadata(mentorId)?.data?.name ?: "",
                    memberIcon = "None",
                    isAdmin = false,
                    isOnline = false,
                    groupId = groupId
                ))
            } else if (event == "delete") {
                database.groupMemberDao().deleteMemberFromGroup(groupId, mentorId)
            }
        }
    }

    override fun messageAction(pubnub: PubNub, pnMessageActionResult: PNMessageActionResult) {}

    override fun file(pubnub: PubNub, pnFileEventResult: PNFileEventResult) {}
}