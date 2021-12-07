package com.joshtalks.joshskills.ui.group.lib

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

import com.google.gson.Gson
import com.joshtalks.joshskills.core.Event
import com.joshtalks.joshskills.core.PrefManager
import com.joshtalks.joshskills.core.notification.FCM_TOKEN
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.ui.group.model.*
import com.joshtalks.joshskills.ui.group.utils.getMessageType

import com.pubnub.api.PubNub
import com.pubnub.api.PNConfiguration
import com.pubnub.api.callbacks.SubscribeCallback
import com.pubnub.api.endpoints.objects_api.utils.Include
import com.pubnub.api.enums.PNLogVerbosity

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jetbrains.annotations.NotNull

import com.pubnub.api.enums.PNPushType
import kotlin.Exception


private const val TAG = "PubNub_Service"

object PubNubService : ChatService {

    private val onlineCountLiveData = MutableLiveData(Event(-1))
    private val pubnub by lazy {
        config.publishKey = "pub-c-07a21ffa-a9e8-45af-93d3-256bb6b4bdd0"
        config.subscribeKey = "sub-c-308b8df2-4cfc-11ec-a76f-16acaa066210"
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

    override fun createGroup(groupName: String, imageUrl: String) {}

    override fun getOnlineCount(groupName: String): LiveData<Event<Int>> = onlineCountLiveData

    override fun getMembersCount(groupName: String): LiveData<Event<Int>> = onlineCountLiveData

    override fun fetchGroupList(pageInfo: PageInfo?): NetworkData? {
        Log.d(TAG, "fetchGroupList: $pageInfo")
        val data = if (pageInfo == null)
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
        return if (data == null) null else PubNubNetworkData(data)
    }

    override fun getUnreadMessageCount(groupId: String, lastSeenTimestamp: Long): Long {
        val count = pubnub.messageCounts()
            .channels(listOf(groupId))
            .channelsTimetoken(listOf(lastSeenTimestamp))
            .sync()
        Log.d(TAG, "getUnreadMessageCount: ${pubnub.timestamp.toLong()}")
        return count?.channels?.get(groupId) ?: 0L
    }

    override fun dispatchNotifications(groups: List<String>) {
        if(groups.isNotEmpty())
            pubnub.addPushNotificationsOnChannels()
                .pushType(PNPushType.FCM)
                .deviceId(PrefManager.getStringValue(FCM_TOKEN))
                .channels(groups)
                .sync()
    }

    override fun getLastMessageDetail(groupId: String): Pair<String, Long> {
        val msg = pubnub.fetchMessages()
            .channels(listOf(groupId))
            .includeMeta(true)
            .includeUUID(true)
            .start(System.currentTimeMillis() * 10_000L)
            .maximumPerChannel(1)
            .sync()

        val message = try {
            val messageItem = Gson().fromJson(msg?.channels?.get(groupId)?.get(0)?.message, MessageItem::class.java)
            messageItem.msg
        } catch (e: Exception) {
            msg?.channels?.get(groupId)?.get(0)?.message?.asString ?: ""
        }
        Log.d(TAG, "getLastDetailsMessage: ${pubnub.timestamp.toLong()}")
        return "${msg?.channels?.get(groupId)?.get(0)?.meta?.asString}: ${message}" to (msg?.channels?.get(groupId)?.get(0)?.timetoken ?: 0L)
    }

    override fun getMessageHistory(groupId: String, startTime : Long?) : List<ChatItem> {
        val history = pubnub.history()
            .channel(groupId)
            .includeMeta(true)
            .includeTimetoken(true)
            .start(startTime ?: System.currentTimeMillis() * 10_000L)
            .count(20)
            .sync()
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
            } catch (e : Exception) {
                e.printStackTrace()
            }
        }
        return messages
    }

    override fun getUnreadMessages(groupId: String, startTime: Long): List<ChatItem> {
        val history = pubnub.history()
            .channel(groupId)
            .includeMeta(true)
            .includeTimetoken(true)
            .start(startTime)
            .end(System.currentTimeMillis() * 10_000L)
            .count(20)
            .sync()
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
            } catch (e : Exception) {
                e.printStackTrace()
            }
        }
        return messages
    }

    override fun sendMessage(groupName: String, messageItem: MessageItem) {
        CoroutineScope(Dispatchers.IO).launch {
            pubnub.publish()
                .channel(groupName)
                .message(messageItem)
                .meta("${Mentor.getInstance().getUser()?.firstName}")
                .shouldStore(true)
                .usePOST(true)
                .sync()
        }
    }

    override fun sendGroupNotification(groupId: String, messageItem: Map<String, Any?>) {
        CoroutineScope(Dispatchers.IO).launch {
            pubnub.publish()
                .channel(groupId)
                .shouldStore(false)
                .message(messageItem)
                .usePOST(true)
                .sync()
        }
    }

    override fun getChannelMembers(groupId: String, adminId: String): MemberResult {
        val memberResult = pubnub.channelMembers
            .channel(groupId)
            .limit(512)
            .includeTotalCount(true)
            .includeUUID(Include.PNUUIDDetailsLevel.UUID)
            .sync()

        val memberCount = memberResult?.totalCount
        var onlineCount = 0

        val memberList = mutableListOf<GroupMember>()
        memberResult?.data?.map {
            val status = getCurrentPresence(it.uuid.id, groupId)
            memberList.add(GroupMember(
                mentorID = it.uuid.id,
                memberName = it.uuid.name,
                memberIcon = it.uuid.profileUrl,
                isAdmin = adminId == it.uuid.id,
                isOnline = getCurrentPresence(it.uuid.id, groupId)
            ))
            if(status) onlineCount++
        }
        memberList.sortByDescending { it.isOnline }
        return MemberResult(memberList, memberCount, onlineCount)
    }

    override fun setMemberPresence(groups: List<String>, isOnline: Boolean) {
        pubnub.setPresenceState()
            .channels(groups)
            .state(mapOf("is_online" to isOnline))
            .sync()
    }

    fun getCurrentPresence(mentorId: String, groupId: String): Boolean {
        val presence = pubnub.presenceState
            .channels(listOf(groupId))
            .uuid(mentorId)
            .sync()

        presence?.stateByUUID?.map {
            return it.value.asJsonObject.get("is_online") != null
        }
        return false
    }

    private fun getOnlineMember(groupName: String): Int {
        val count = pubnub.hereNow()
            .channels(listOf(groupName))
            .sync()
        return count?.channels?.get(groupName)?.occupancy ?: 0
    }
}

