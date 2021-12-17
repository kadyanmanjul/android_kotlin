package com.joshtalks.joshskills.ui.group.model

import com.pubnub.api.models.consumer.objects_api.member.PNMembers

data class GroupMember(
    val mentorID: String,
    val memberName: String,
    val memberIcon: String,
    val isAdmin: Boolean,
    var isOnline: Boolean
) {
    fun getMemberURL() = if (memberIcon == "None") "" else memberIcon
}

class MemberResult(val list: MutableList<PNMembers>, val memberCount: Int?)