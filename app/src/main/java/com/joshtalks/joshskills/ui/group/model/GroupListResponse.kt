package com.joshtalks.joshskills.ui.group.model

import androidx.room.Entity
import androidx.room.PrimaryKey

import com.google.gson.annotations.SerializedName
import com.joshtalks.joshskills.core.Utils
import java.util.Date

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

    val lastMessage: String? = null,

    val lastMsgTime: Long = 0,

    val unreadCount: String? = null,

    @field:SerializedName("name")
    val name: String? = null,

    @field:SerializedName("created_by")
    val createdBy: String? = null,

    @field:SerializedName("total_calls")
    val totalCalls: String? = null,

    val adminId: String? = null

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

    override fun getLastMessageTime() =
        if (lastMsgTime == 0L) Utils.getMessageTimeInHours(Date(createdAt?.times(1000)!!))
        else Utils.getMessageTimeInHours(Date(lastMsgTime / 10000))

    override fun getUnreadMsgCount() =
        if (unreadCount == "0" || unreadCount == null) ""
        else unreadCount
}
