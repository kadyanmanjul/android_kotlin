package com.joshtalks.joshskills.groups.model

import com.google.gson.annotations.SerializedName

data class LeaveGroupRequest (

    @field:SerializedName("group_id")
    val groupId: String,

    @field:SerializedName("mentor_id")
    val mentorId: String
)