package com.joshtalks.joshskills.ui.group.model

import com.google.gson.annotations.SerializedName

data class LeaveGroupRequest (

    @field:SerializedName("group_id")
    val groupId: String,

    @field:SerializedName("mentor_id")
    val mentorId: String
)