package com.joshtalks.joshskills.ui.group.model

import com.google.gson.annotations.SerializedName

data class GroupJoinRequest (

    @field:SerializedName("mentor_id")
    val mentorId: String,

    @field:SerializedName("group_id")
    val groupId: String,

    @field:SerializedName("answer")
    val answer: String
)