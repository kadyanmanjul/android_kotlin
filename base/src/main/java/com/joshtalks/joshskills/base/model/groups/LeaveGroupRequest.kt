package com.joshtalks.joshskills.base.model.groups

import com.google.gson.annotations.SerializedName

data class LeaveGroupRequest (

    @field:SerializedName("group_id")
    val groupId: String,

    @field:SerializedName("mentor_id")
    val mentorId: String
)