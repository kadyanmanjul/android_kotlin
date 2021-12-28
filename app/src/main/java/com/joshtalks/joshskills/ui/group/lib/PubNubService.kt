package com.joshtalks.joshskills.ui.group.lib

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.gson.Gson
import com.joshtalks.joshskills.core.Event
import com.joshtalks.joshskills.repository.local.AppDatabase
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.ui.group.model.ChatItem
import com.joshtalks.joshskills.ui.group.model.MessageItem
import com.joshtalks.joshskills.ui.group.model.PageInfo
import com.joshtalks.joshskills.ui.group.model.PubNubNetworkData
import com.joshtalks.joshskills.ui.group.utils.getMessageType
import com.pubnub.api.PubNub
import com.pubnub.api.PNConfiguration
import com.pubnub.api.callbacks.SubscribeCallback
import com.pubnub.api.endpoints.objects_api.utils.Include
import com.pubnub.api.enums.PNLogVerbosity
import com.pubnub.api.models.consumer.PNStatus
import com.pubnub.api.models.consumer.objects_api.channel.PNChannelMetadataResult
import com.pubnub.api.models.consumer.objects_api.membership.PNMembershipResult
import com.pubnub.api.models.consumer.objects_api.uuid.PNUUIDMetadataResult
import com.pubnub.api.models.consumer.pubsub.PNMessageResult
import com.pubnub.api.models.consumer.pubsub.PNPresenceEventResult
import com.pubnub.api.models.consumer.pubsub.PNSignalResult
import com.pubnub.api.models.consumer.pubsub.files.PNFileEventResult
import com.pubnub.api.models.consumer.pubsub.message_actions.PNMessageActionResult
import java.lang.Exception
import java.sql.Timestamp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jetbrains.annotations.NotNull

private const val TAG = "PubNub_Service"

class PubNubService private constructor(val groupName: String?) : ChatService {

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

    companion object {
        fun getChatService(groupName: String? = null): ChatService {
            return PubNubService(groupName)
        }
    }

    override fun initializeChatService() {
        reset()
        setInitialState()
    }

    override fun <T> subscribeToChatEvents(groups: List<String>, observer: ChatEventObserver<T>) {
        reset()
        pubnub.addListener(observer.getObserver() as @NotNull SubscribeCallback)
        Log.d(TAG, "subscribeToChatEvents: ${pubnub.timestamp}")
        Log.d(TAG, "subscribeToChatEvents: ${pubnub.timestamp.minus(24 * 60 * 60L)}")
        pubnub.subscribe()
            //.withPresence()
            //.withTimetoken(pubnub.timestamp.minus(12 * 60 * 60L) * 1000L)
            .channels(groups)
            .execute()
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

    override fun getLastMessageDetail(groupId: String): Pair<String, Long> {
        val msg = pubnub.fetchMessages()
            .channels(listOf(groupId))
            .includeMeta(true)
            .includeUUID(true)
            .end(pubnub.timestamp.toLong() * 1000)
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

    override fun getMessageHistory(groupId: String, timeToken : Long?) : List<ChatItem> {
        val history = pubnub.history()
            .channel(groupName)
            .includeMeta(true)
            .includeTimetoken(true)
            .end(timeToken ?: System.currentTimeMillis() * 1000L)
            .count(20)
            .sync()
        val messages = mutableListOf<ChatItem>()
        history?.messages?.map {
            val messageItem = Gson().fromJson(it.entry.asJsonObject, MessageItem::class.java)
            val message = ChatItem(
                sender = it.meta.asString,
                msgType = messageItem.getMessageType(),
                message = messageItem.msg,
                msgTime = it.timetoken,
                groupId = groupId
            )
            messages.add(message)
        }
        return messages
    }

    override fun sendMessage(messageItem: MessageItem) {
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

    private fun getOnlineMember(groupName: String): Int {
        val count = pubnub.hereNow()
            .channels(listOf(groupName))
            .sync()
        return count?.channels?.get(groupName)?.occupancy ?: 0
    }

    private fun reset() {
        //pubnub.removeListener(subscribeCallback)
        pubnub.unsubscribeAll()
    }

    // TODO: Need to refactor the name
    private fun setInitialState() {
        //pubnub.addListener(subscribeCallback)
        // TODO: Refactor
        CoroutineScope(Dispatchers.IO).launch {

        }
        pubnub.subscribe()
            .withPresence()
            //.withTimetoken()
            .execute()
    }
}

