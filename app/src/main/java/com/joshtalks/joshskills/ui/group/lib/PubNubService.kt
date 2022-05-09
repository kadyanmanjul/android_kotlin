package com.joshtalks.joshskills.ui.group.lib

import android.util.Log
import com.google.gson.Gson
import com.joshtalks.joshskills.BuildConfig
import com.joshtalks.joshskills.core.PrefManager
import com.joshtalks.joshskills.core.notification.FCM_TOKEN
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.ui.group.model.ChatItem
import com.joshtalks.joshskills.ui.group.model.MessageItem
import com.joshtalks.joshskills.ui.group.utils.getMessageType
import com.pubnub.api.PNConfiguration
import com.pubnub.api.PubNub
import com.pubnub.api.callbacks.SubscribeCallback
import com.pubnub.api.endpoints.objects_api.utils.Include
import com.pubnub.api.enums.PNLogVerbosity
import com.pubnub.api.enums.PNPushType
import com.pubnub.api.models.consumer.history.PNHistoryResult
import com.pubnub.api.models.consumer.objects_api.member.PNGetChannelMembersResult
import com.pubnub.api.models.consumer.objects_api.membership.PNGetMembershipsResult
import com.pubnub.api.models.consumer.objects_api.uuid.PNGetUUIDMetadataResult
import com.pubnub.api.models.consumer.presence.PNHereNowOccupantData
import java.util.stream.Collectors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jetbrains.annotations.NotNull

private const val TAG = "PubNubService"

object PubNubService : ChatService {

    private val pubnub by lazy {
        config.publishKey = BuildConfig.PUBNUB_PUB_GROUPS_KEY
        config.subscribeKey = BuildConfig.PUBNUB_SUB_GROUPS_KEY
        config.uuid = Mentor.getInstance().getId()
        PubNub(config)
    }

    private val config by lazy {
        PNConfiguration().apply {
            logVerbosity = PNLogVerbosity.BODY
        }
    }

    override fun initializeChatService() {}

    override fun <T> subscribeToChatEvents(groups: List<String>, observer: ChatEventObserver<T>) {
        pubnub.addListener(observer.getObserver() as @NotNull SubscribeCallback)
        pubnub.subscribe()
            .withPresence()
            .channels(groups)
            .execute()
    }

    override fun <T> unsubscribeToChatEvents(observer: ChatEventObserver<T>) {
        pubnub.removeListener(observer.getObserver() as @NotNull SubscribeCallback)
        pubnub.unsubscribeAll()
    }

    override fun fetchGroupList(pageInfo: PageInfo?): NetworkData? {
        Log.d(TAG, "fetchGroupList: $pageInfo")
        val data: PNGetMembershipsResult?
        try {
            data = if (pageInfo == null)
                pubnub.memberships
                    .includeChannel(Include.PNChannelDetailsLevel.CHANNEL_WITH_CUSTOM)
                    .includeCustom(true)
                    .limit(10)
                    .sync()
            else
                pubnub.memberships
                    .includeChannel(Include.PNChannelDetailsLevel.CHANNEL_WITH_CUSTOM)
                    .includeCustom(true)
                    .limit(10)
                    .page(pageInfo.pubNubNext ?: pageInfo.pubNubPrevious)
                    .sync()
        } catch (e: Exception) {
            return null
        }
        return if (data == null) null else PubNubNetworkData(data)
    }

    override fun getGroupMemberList(groupId: String, pageInfo: PageInfo?): MemberNetworkData? {
        val memberResult: PNGetChannelMembersResult?
        try {
            memberResult = if (pageInfo == null) {
                pubnub.channelMembers
                    .channel(groupId)
                    .limit(100)
                    .includeUUID(Include.PNUUIDDetailsLevel.UUID)
                    .sync()
            } else {
                pubnub.channelMembers
                    .channel(groupId)
                    .limit(100)
                    .page(pageInfo.pubNubNext ?: pageInfo.pubNubPrevious)
                    .includeUUID(Include.PNUUIDDetailsLevel.UUID)
                    .sync()
            }
        } catch (e: Exception) {
            return null
        }
        return if (memberResult == null) null else PubNubMemberData(memberResult)
    }

    override fun getUnreadMessageCount(groupId: String, lastSeenTimestamp: Long): Long {
        val count = pubnub.messageCounts()
            .channels(listOf(groupId))
            .channelsTimetoken(listOf(lastSeenTimestamp))
            .sync()

        return count?.channels?.get(groupId) ?: 0L
    }

    override fun dispatchNotifications(groups: List<String>) {
        try {
            if (groups.isNotEmpty())
                pubnub.addPushNotificationsOnChannels()
                    .pushType(PNPushType.FCM)
                    .deviceId(PrefManager.getStringValue(FCM_TOKEN))
                    .channels(groups)
                    .sync()
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e(TAG, "Error in dispatching notifications")
        }
    }

    override fun getLastMessageDetail(groupId: String): Pair<String, Long> {
        val msg = pubnub.fetchMessages()
            .channels(listOf(groupId))
            .includeMeta(true)
            .includeUUID(true)
            .start(System.currentTimeMillis() * 10_000L)
            .maximumPerChannel(1)
            .sync()

        val msgObj = msg?.channels?.get(groupId)?.get(0)

        val message = try {
            Gson().fromJson(msgObj?.message, MessageItem::class.java).msg
        } catch (e: Exception) {
            msgObj?.message?.asString ?: ""
        }

        return if (msgObj?.message?.asJsonObject?.get("msgType")?.asInt == 1) {
            if (msgObj.uuid == Mentor.getInstance().getId())
                message.replace("${msgObj.meta?.asString} has", "You have") to (msgObj.timetoken ?: 0L)
            else
                message to (msgObj.timetoken ?: 0L)
        } else {
            if (msgObj?.uuid == Mentor.getInstance().getId())
                "You: $message" to (msgObj.timetoken ?: 0L)
            else
                "${msgObj?.meta?.asString}: $message" to (msgObj?.timetoken ?: 0L)
        }
    }

    override fun getMessageHistory(groupId: String, startTime: Long?): List<ChatItem> {
        var history: PNHistoryResult? = null
        try {
            history = pubnub.history()
                .channel(groupId)
                .includeMeta(true)
                .includeTimetoken(true)
                .start(startTime ?: System.currentTimeMillis() * 10_000L)
                .count(20)
                .sync()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val messages = mutableListOf<ChatItem>()

        history?.messages?.map {
            try {
                val messageItem = Gson().fromJson(it.entry.asJsonObject, MessageItem::class.java)
                val message = ChatItem(
                    sender = it.meta.asString,
                    msgType = messageItem.getMessageType(),
                    message = messageItem.msg,
                    msgTime = it.timetoken,
                    groupId = groupId,
                    messageId = "${it.timetoken}_${groupId}_${messageItem.mentorId}"
                )
                messages.add(message)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return messages
    }

    override fun getUnreadMessages(groupId: String, startTime: Long): List<ChatItem> {
        var history: PNHistoryResult? = null
        try {
            history = pubnub.history()
                .channel(groupId)
                .includeMeta(true)
                .includeTimetoken(true)
                .start(startTime)
                .end(System.currentTimeMillis() * 10_000L)
                .count(20)
                .sync()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        val messages = mutableListOf<ChatItem>()

        history?.messages?.map {
            try {
                val messageItem = Gson().fromJson(it.entry.asJsonObject, MessageItem::class.java)
                val message = ChatItem(
                    sender = it.meta.asString,
                    msgType = messageItem.getMessageType(),
                    message = messageItem.msg,
                    msgTime = it.timetoken,
                    groupId = groupId,
                    messageId = "${it.timetoken}_${groupId}_${messageItem.mentorId}"
                )
                messages.add(message)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return messages
    }

    override fun sendMessage(groupName: String, messageItem: MessageItem) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                pubnub.publish()
                    .channel(groupName)
                    .message(messageItem)
                    .meta("${Mentor.getInstance().getUser()?.firstName}")
                    .shouldStore(true)
                    .ttl(0)
                    .usePOST(true)
                    .sync()
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }

    override fun sendGroupNotification(groupId: String, messageItem: Map<String, Any?>) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                pubnub.publish()
                    .channel(groupId)
                    .shouldStore(false)
                    .message(messageItem)
                    .usePOST(true)
                    .sync()
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }

    override fun getPubNubOnlineMembers(groupId: String): List<String>? = try {
        pubnub.hereNow()
            .channels(listOf(groupId))
            .sync()?.channels?.get(groupId)?.occupants
            ?.stream()?.map(PNHereNowOccupantData::getUuid)
            ?.collect(Collectors.toList())
    } catch (e: Exception) {
        null
    }

    override fun getUserMetadata(mentorId: String): PNGetUUIDMetadataResult? = try {
        pubnub.uuidMetadata
            .uuid(mentorId)
            .includeCustom(true)
            .sync()
    } catch (e: Exception) {
        null
    }
}

