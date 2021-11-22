package com.joshtalks.joshskills.ui.group.model

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import com.bumptech.glide.util.Util
import com.google.gson.annotations.SerializedName
import com.joshtalks.joshskills.core.Utils
import kotlinx.android.parcel.Parcelize

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

	val lastMessage : String? = null,

	val lastMsgTime: String? = null,

	val unreadCount: String? = null,

	@field:SerializedName("name")
	val name: String? = null,

	@field:SerializedName("created_by")
	val createdBy: String? = null,

	@field:SerializedName("total_calls")
	val totalCalls: String? = null

) : GroupItemData {

	override fun getTitle() = name ?: ""

	override fun getSubTitle() = lastMessage ?: "$totalCalls"//"$totalCalls practise partner calls in last 24 hours"

	override fun getUniqueId() = groupId ?: ""

	override fun getImageUrl() = groupIcon ?: ""

	override fun getCreatedTime() = if(createdAt == null) "" else Utils.getMessageTime(createdAt * 1000L, timeNeeded = false)

	override fun getCreator() = createdBy ?: ""

	override fun hasJoined() = lastMessage != null

	override fun getLastMessageTime() = lastMsgTime ?: ""

	override fun getUnreadMsgCount() = unreadCount ?: ""
}
