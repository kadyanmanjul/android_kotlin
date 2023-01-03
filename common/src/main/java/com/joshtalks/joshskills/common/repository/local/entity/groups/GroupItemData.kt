package com.joshtalks.joshskills.common.repository.local.entity.groups

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
import com.joshtalks.joshskills.common.core.EMPTY
import com.joshtalks.joshskills.common.core.Utils
import kotlinx.parcelize.Parcelize

const val OPENED_GROUP = "open"
const val CLOSED_GROUP = "closed"

const val REQUESTED_GROUP = "requested"
const val JOINED_GROUP = "joined"
const val NOT_JOINED_GROUP = "not_joined"

interface GroupItemData {
    fun getTitle() : String
    fun getSubTitle() : String
    fun getUniqueId() : String
    fun getImageUrl() : String
    fun getCreator() : String
    fun getCreatorId() : String
    fun getLastMessageTime() : String
    fun getUnreadMsgCount() : String
    fun getGroupCategory() : String
    fun getJoinedStatus() : String
    fun getGroupText() : String
    fun getAgoraId() : Int
    fun getLastMessageText() : String

    //TODO: Need to remove
    fun hasJoined() : Boolean
}

@Entity(tableName = "group_list_table")
@Parcelize
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

    @field:SerializedName("mentor_id")
    val adminId: String? = null,

    @field:SerializedName("group_type")
    val groupType: String? = OPENED_GROUP,

    @field:SerializedName("group_status")
    val groupStatus: String? = null,

    @field:SerializedName("closed_grp_text")
    val requestGroupText: String? = null,

    @field:SerializedName("agora_id")
    val agoraUid: Int? = null

) : Parcelable, GroupItemData {

    override fun getTitle() = name ?: ""

    override fun getSubTitle() = lastMessage ?: "$totalCalls"

    override fun getUniqueId() = groupId

    override fun getImageUrl() = groupIcon ?: ""

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

    override fun getUnreadMsgCount() = unreadCount ?: "0"

    override fun getGroupCategory() = groupType ?: OPENED_GROUP

    override fun getJoinedStatus(): String {
        return when {
            groupStatus == REQUESTED_GROUP -> REQUESTED_GROUP
            hasJoined() -> JOINED_GROUP
            else -> NOT_JOINED_GROUP
        }
    }

    override fun getGroupText() = requestGroupText ?: ""

    override fun getAgoraId() = agoraUid ?: 0

    override fun getLastMessageText() = lastMessage ?: EMPTY
}
