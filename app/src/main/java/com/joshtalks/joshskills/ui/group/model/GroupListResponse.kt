package com.joshtalks.joshskills.ui.group.model

import androidx.room.Entity
import androidx.room.PrimaryKey

import com.google.gson.annotations.SerializedName
import com.joshtalks.joshskills.core.Utils

data class GroupListResponse(

    @field:SerializedName("groups")
    val groups: List<GroupsItem?>? = null
)

@Entity(tableName = "group_list_table")
data class GroupsItem(

    @field:SerializedName("group_icon")
    val groupIcon: String? = null,

    @PrimaryKey
    @field:SerializedName("group_id")
    val groupId: String,

    @field:SerializedName("created_at")
    val createdAt: Long? = null,

    var lastMessage: String? = null,

    var lastMsgTime: Long = 0,

    var unreadCount: String? = null,

    @field:SerializedName("name")
    val name: String? = null,

    @field:SerializedName("created_by")
    val createdBy: String? = null,

    @field:SerializedName("total_calls")
    val totalCalls: String? = null,

    val adminId: String? = null,

    val lastSentMsgTime: Long = 0

) : GroupItemData {

    override fun getTitle() = name ?: ""

    override fun getSubTitle() =
        lastMessage ?: "$totalCalls"//"$totalCalls practise partner calls in last 24 hours"

    override fun getUniqueId() = groupId

    override fun getImageUrl() = groupIcon ?: ""

    override fun getCreatedTime() =
        if (createdAt == null) "" else Utils.getMessageTime(createdAt * 1000L, timeNeeded = false)

    override fun getCreator() = createdBy ?: ""

    override fun getCreatorId() = adminId ?: ""

    override fun hasJoined() = lastMessage != null

    override fun getLastMessageTime(): String {
        val time = if (lastMsgTime == 0L)
            Utils.getMessageTime(createdAt?.times(1000)!!)
        else
            Utils.getMessageTime(lastMsgTime / 10000)

        return if (time[0] == '0') time.substring(1) else time
    }

    override fun getUnreadMsgCount() =
        unreadCount ?: "0"
}
