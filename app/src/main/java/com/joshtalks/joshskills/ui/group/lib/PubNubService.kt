package com.joshtalks.joshskills.ui.group.lib

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import com.joshtalks.joshskills.core.Event
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.ui.group.model.GroupItemData
import com.joshtalks.joshskills.ui.group.model.PageInfo
import com.joshtalks.joshskills.ui.group.model.PubNubNetworkData
import com.pubnub.api.PubNub
import com.pubnub.api.PNConfiguration
import com.pubnub.api.callbacks.SubscribeCallback
import com.pubnub.api.endpoints.objects_api.utils.Include
import com.pubnub.api.models.consumer.PNStatus
import com.pubnub.api.models.consumer.objects_api.channel.PNChannelMetadataResult
import com.pubnub.api.models.consumer.objects_api.channel.PNGetAllChannelsMetadataResult
import com.pubnub.api.models.consumer.objects_api.membership.PNMembershipResult
import com.pubnub.api.models.consumer.objects_api.uuid.PNUUIDMetadataResult
import com.pubnub.api.models.consumer.pubsub.PNMessageResult
import com.pubnub.api.models.consumer.pubsub.PNPresenceEventResult
import com.pubnub.api.models.consumer.pubsub.PNSignalResult
import com.pubnub.api.models.consumer.pubsub.files.PNFileEventResult
import com.pubnub.api.models.consumer.pubsub.message_actions.PNMessageActionResult
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private const val TAG = "PubNub_Service"
class PubNubService private constructor(val groupName: String?): ChatService {
    private val onlineCountLiveData = MutableLiveData(Event(-1))
    private val pubnub by lazy {
        config.publishKey = "pub-c-b8485bb5-70b0-4a50-9e4c-46352847fbea"
        config.subscribeKey = "sub-c-5807b5bc-30d3-11ec-ab4c-7aa074450caf"
        config.uuid = Mentor.getInstance().getId()
        PubNub(config)
    }
    private val config by lazy {
        PNConfiguration()
    }

    private val subscribeCallback = object : SubscribeCallback() {
        override fun status(pubnub: PubNub, pnStatus: PNStatus) {

        }

        override fun message(pubnub: PubNub, pnMessageResult: PNMessageResult) {

        }

        override fun presence(pubnub: PubNub, pnPresenceEventResult: PNPresenceEventResult) {}

        override fun signal(pubnub: PubNub, pnSignalResult: PNSignalResult) {}

        override fun uuid(pubnub: PubNub, pnUUIDMetadataResult: PNUUIDMetadataResult) {}

        override fun channel(pubnub: PubNub, pnChannelMetadataResult: PNChannelMetadataResult) {}

        override fun membership(pubnub: PubNub, pnMembershipResult: PNMembershipResult) {}

        override fun messageAction(
            pubnub: PubNub,
            pnMessageActionResult: PNMessageActionResult
        ) {}

        override fun file(pubnub: PubNub, pnFileEventResult: PNFileEventResult) {}
    }

    companion object {
        fun getChatService(groupName: String? = null) : ChatService {
            return PubNubService(groupName)
        }
    }

    override fun initializeChatService() {
        reset()
        setInitialState()
    }

    override fun createGroup(groupName: String, imageUrl: String) {}

    override fun getOnlineCount(groupName: String): LiveData<Event<Int>> = onlineCountLiveData

    override fun getMembersCount(groupName: String): LiveData<Event<Int>> = onlineCountLiveData

    override fun fetchGroupList(pageInfo: PageInfo?): NetworkData? {
        Log.d(TAG, "fetchGroupList: $pageInfo")
        val data = if(pageInfo == null)
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
        return if(data  == null) null else PubNubNetworkData(data)
    }

    override fun getUnreadMessageCount(groupName: String): Long {
        val count = pubnub.messageCounts().channels(listOf(groupName)).sync()
        return count?.channels?.get(groupName) ?: 0L
    }

    override fun getLastMessage(groupName: String): String {
        val msg = pubnub.fetchMessages()
            .channels(listOf(groupName))
            .includeMeta(true)
            .includeUUID(true)
            .end(System.currentTimeMillis())
            .maximumPerChannel(1)
            .sync()
        return "${msg?.channels?.get(groupName)?.get(0)?.meta?.asString}: ${msg?.channels?.get(groupName)?.get(0)?.message?.asString}"
    }

    override fun sendMessage(msg: String) {
        CoroutineScope(Dispatchers.IO).launch {
            pubnub.publish()
                .channel(groupName)
                .message(msg)
                .meta("${Mentor.getInstance().getUser()?.firstName}")
                .shouldStore(true)
                .usePOST(true)
                .sync()
        }
    }


    private fun getOnlineMember(groupName: String) : Int {
        val count = pubnub.hereNow()
            .channels(listOf(groupName))
            .sync()
        return count?.channels?.get(groupName)?.occupancy ?: 0
    }

    private fun reset() {
        pubnub.removeListener(subscribeCallback)
        pubnub.unsubscribeAll()
    }

    // TODO: Need to refactor the name
    private fun setInitialState() {
        pubnub.addListener(subscribeCallback)
        pubnub.subscribe()
            .withPresence()
            .channels(listOf(groupName))
            .execute()
    }
}

