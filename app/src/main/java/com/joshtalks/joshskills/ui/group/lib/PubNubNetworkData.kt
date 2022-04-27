package com.joshtalks.joshskills.ui.group.lib

import android.util.Log
import com.google.gson.JsonObject
import com.joshtalks.joshskills.core.EMPTY
import com.joshtalks.joshskills.core.showToast
import com.joshtalks.joshskills.ui.group.constants.DM_CHAT
import com.joshtalks.joshskills.ui.group.constants.OPENED_GROUP
import com.joshtalks.joshskills.ui.group.model.GroupListResponse
import com.joshtalks.joshskills.ui.group.model.GroupsItem
import com.joshtalks.joshskills.ui.group.repository.GroupRepository
import com.pubnub.api.models.consumer.objects_api.membership.PNGetMembershipsResult

private const val TAG = "PubNub_NetworkData"

data class PubNubNetworkData(val data: PNGetMembershipsResult) : NetworkData {

    val groupList = mutableListOf<GroupsItem>()
    private val chatService: ChatService = PubNubService
    val repository = GroupRepository()

    override fun getData(): GroupListResponse {

        groupList.clear()
        for (group in data.data) {
            try {
                val channelCustom = group.channel.custom as JsonObject
                val channelMembershipCustom = group.custom as JsonObject
                Log.e("sagar", "getData: $channelMembershipCustom")
                val customMap = getCustomMap(channelCustom, channelMembershipCustom)
                val (lastMsg, lastMessageTime) = chatService.getLastMessageDetail(group.channel.id,customMap["group_type"] ?: OPENED_GROUP)

                val response = GroupsItem(
                    groupId = group.channel.id,
                    name = getGroupName(
                        group.channel.name?: EMPTY,
                        customMap["group_type"],
                        channelMembershipCustom["channel_name"]?.asString ?: EMPTY
                    ),
                    lastMessage = lastMsg,
                    lastMsgTime = lastMessageTime,
                    unreadCount = chatService.getUnreadMessageCount(
                        group.channel.id,
                        getTimeToken(channelMembershipCustom["time_token"].asLong, group.channel.id)
                    ).toString(),
                    groupIcon = if (channelMembershipCustom["image_url"]?.asString == "None")
                        EMPTY
                    else
                        channelMembershipCustom["image_url"]?.asString ?: EMPTY
                    ,
                    createdAt = customMap["created_at"]?.toLongOrNull(),
                    createdBy = customMap["created_by"],
                    adminId = customMap["admin_id"],
                    groupType = customMap["group_type"] ?: OPENED_GROUP,
                    agoraUid = (customMap["agora_id"]?.toInt() ?: 0),
                    dmPartnerMentorId = customMap["mentor_id"]?: EMPTY
                )
                if ((customMap["group_type"] == DM_CHAT && lastMsg == EMPTY).not())
                    groupList.add(response)
            } catch (e: Exception) {
                e.printStackTrace()
                Log.e("sagar", "Error in group : ${group.channel.id} : ${group.channel.name} :${e.message}")
                showToast("An error has occurred")
            }
        }
        return GroupListResponse(groups = groupList.toList())
    }

    override fun getPageInfo() = PageInfo(
        pubNubPrevious = data.previousPage(),
        pubNubNext = data.nextPage(),
    )

    private fun getCustomMap(json: JsonObject, membership: JsonObject): Map<String, String> {
        val map = mutableMapOf<String, String>()
        json.get("")
        map["created_at"] = json["created_at"].asString
        map["created_by"] = json["created_by"].asString
        map["image_url"] = json["image_url"]?.asString ?: EMPTY
        map["admin_id"] = json["mentor_id"].asString
        map["group_type"] = json["group_type"]?.asString ?: OPENED_GROUP
        if (map["group_type"] == DM_CHAT) {
            map["mentor_id"] = membership["mentor_id"].asString
            map["agora_id"] = membership["agora_id"].asString
        }
        return map
    }

    fun getTimeToken(pubnubTime: Long, id: String) = repository.getRecentTimeToken(id) ?: pubnubTime

    private fun getGroupName(name: String?, groupType: String?, dmName: String?): String? =
        if (groupType == DM_CHAT) dmName
        else name

    private fun getGroupIcon(icon: String?, groupType: String?, dmIcon: String?): String? =
        if (groupType == DM_CHAT) dmIcon
        else icon
}