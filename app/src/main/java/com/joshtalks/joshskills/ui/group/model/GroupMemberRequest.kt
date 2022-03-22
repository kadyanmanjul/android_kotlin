package com.joshtalks.joshskills.ui.group.model

import com.google.gson.annotations.SerializedName

class GroupMemberRequest(

    @field:SerializedName("mentor_id")
    val mentorId: String,

    @field:SerializedName("name")
    val memberName: String,

    @field:SerializedName("profile_url")
    val profileUrl: String,

    @field:SerializedName("answer_to_join")
    val answer: String,
) {
    fun getMemberURL() = if (profileUrl == "None") "" else profileUrl
}