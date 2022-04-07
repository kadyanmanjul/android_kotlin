package com.joshtalks.joshskills.ui.group.model

import androidx.room.Entity

@Entity(tableName = "group_member_table", primaryKeys = ["mentorID", "groupId"])
data class GroupMember(
    val mentorID: String,
    val memberName: String,
    val memberIcon: String,
    val isAdmin: Boolean,
    var isOnline: Boolean,
    val groupId: String
) {
    fun getMemberURL() = if (memberIcon == "None") "" else memberIcon
}