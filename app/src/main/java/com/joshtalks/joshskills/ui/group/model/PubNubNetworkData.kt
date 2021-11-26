package com.joshtalks.joshskills.ui.group.model

import android.util.Log
import com.google.gson.JsonObject
import com.joshtalks.joshskills.ui.group.lib.ChatService
import com.joshtalks.joshskills.ui.group.lib.NetworkData
import com.joshtalks.joshskills.ui.group.lib.PubNubService
import com.joshtalks.joshskills.ui.group.repository.GroupRepository
import com.pubnub.api.models.consumer.objects_api.membership.PNGetMembershipsResult

private const val TAG = "PubNub_NetworkData"

data class PubNubNetworkData(val data: PNGetMembershipsResult) : NetworkData {

    val groupList = mutableListOf<GroupsItem>()
    private val chatService : ChatService = PubNubService
    val repository = GroupRepository()

    override fun getData(): GroupListResponse {
        Log.d(TAG, "getData: $data")
        groupList.clear()
        for (group in data.data) {
            val channelCustom = group.channel.custom as JsonObject
            val channelMembershipCustom = group.custom as JsonObject
            val customMap = getCustomMap(channelCustom)
            val (lastMsg, lastMessageTime) = chatService.getLastMessageDetail(group.channel.id)
            Log.d(TAG, "getData: Custom -- $channelCustom")
            val response = GroupsItem(
                groupId = group.channel.id,
                name = group.channel.name,
                lastMessage = lastMsg,
                lastMsgTime = lastMessageTime,
                unreadCount = chatService.getUnreadMessageCount(
                    group.channel.id,
                    getTimeToken(channelMembershipCustom["time_token"].asLong, group.channel.id)
                ).toString(),
                groupIcon = customMap["image_url"],
                createdAt = customMap["created_at"]?.toLongOrNull(),
                createdBy = customMap["created_by"]
            )
            groupList.add(response)
        }
        Log.d(TAG, "getData: $groupList")
        return GroupListResponse(groups = groupList.toList())
    }

    override fun getPageInfo() = PageInfo(
        pubNubPrevious = data.previousPage(),
        pubNubNext = data.nextPage(),
    )

    private fun getCustomMap(json: JsonObject): Map<String, String> {
        val map = mutableMapOf<String, String>()
        json.get("")
        map["created_at"] = json["created_at"].asString
        map["created_by"] = json["created_by"].asString
        map["image_url"] = json["image_url"].asString
        return map
    }

    fun getTimeToken(pubnubTime: Long, id: String) = repository.getRecentTimeToken(id) ?: pubnubTime
}