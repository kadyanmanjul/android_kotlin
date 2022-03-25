package com.joshtalks.joshskills.ui.group.lib

import android.util.Log
import com.google.gson.JsonObject
import com.joshtalks.joshskills.core.showToast
import com.joshtalks.joshskills.ui.group.constants.OPENED_GROUP
import com.joshtalks.joshskills.ui.group.model.GroupListResponse
import com.joshtalks.joshskills.ui.group.model.GroupsItem
import com.joshtalks.joshskills.ui.group.repository.GroupRepository
import com.pubnub.api.models.consumer.objects_api.membership.PNGetMembershipsResult

private const val TAG = "PubNub_NetworkData"

data class PubNubNetworkData(val data: PNGetMembershipsResult) : NetworkData {

    val groupList = mutableListOf<GroupsItem>()
    private val chatService : ChatService = PubNubService
    val repository = GroupRepository()

    override fun getData(): GroupListResponse {

        groupList.clear()
        for (group in data.data) {
            try {
                val channelCustom = group.channel.custom as JsonObject
                val channelMembershipCustom = group.custom as JsonObject
                val customMap = getCustomMap(channelCustom)
                val (lastMsg, lastMessageTime) = chatService.getLastMessageDetail(group.channel.id)

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
                    createdBy = customMap["created_by"],
                    adminId = customMap["admin_id"],
                    groupType = customMap["group_type"] ?: OPENED_GROUP
                )
                groupList.add(response)
            } catch (e: Exception) {
                e.printStackTrace()
                Log.e(TAG, "Error in group : ${group.channel.id} : ${group.channel.name}")
                showToast("An error has occurred")
            }
        }
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
        map["admin_id"] = json["mentor_id"].asString
        map["group_type"] = json["group_type"].asString
        return map
    }

    fun getTimeToken(pubnubTime: Long, id: String) = repository.getRecentTimeToken(id) ?: pubnubTime
}