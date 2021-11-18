package com.joshtalks.joshskills.ui.group.model

import android.util.Log
import com.google.gson.JsonObject
import com.joshtalks.joshskills.core.Utils
import com.joshtalks.joshskills.ui.group.lib.ChatService
import com.joshtalks.joshskills.ui.group.lib.NetworkData
import com.joshtalks.joshskills.ui.group.lib.PubNubService
import com.pubnub.api.models.consumer.objects_api.membership.PNGetMembershipsResult
import java.util.Date

private const val TAG = "PubNub_NetworkData"
data class PubNubNetworkData(val data : PNGetMembershipsResult) : NetworkData {
    val groupList = mutableListOf<GroupsItem>()
    val chatService : ChatService = PubNubService.getChatService()
    override fun getData(): GroupListResponse {
        Log.d(TAG, "getData: $data")
        groupList.clear()
        for (group in data.data) {
            val custom = group.channel.custom as JsonObject
            val customMap = getCustomMap(custom)
            val (lastMsg, lastMessageTime) = chatService.getLastDetailsMessage(group.channel.id)
            Log.d(TAG, "getData: Custum -- $custom")
            val response = GroupsItem(
                groupId = group.channel.id,
                name = group.channel.name,
                lastMessage = lastMsg,
                lastMsgTime = Utils.getMessageTimeInHours(Date(lastMessageTime)),
                unreadCount = chatService.getUnreadMessageCount(group.channel.id).toString(),
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

    private fun getCustomMap(json : JsonObject) : Map<String, String>{
        val map = mutableMapOf<String, String>()
        json.get("")
        map["created_at"] = json["created_at"].asString
        map["created_by"] = json["created_by"].asString
        map["image_url"] = json["image_url"].asString
        return map
    }
}