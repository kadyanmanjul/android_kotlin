package com.joshtalks.joshskills.ui.group.model

data class GroupMember(
    val mentorID: String,
    val memberName: String,
    val memberIcon: String,
    val isAdmin: Boolean,
    var isOnline: Boolean
) {
    fun getMemberURL() = if (memberIcon == "None") "" else memberIcon
}

class MemberResult(val list: MutableList<GroupMember>, val memberCount: Int?, val onlineCount: Int?)