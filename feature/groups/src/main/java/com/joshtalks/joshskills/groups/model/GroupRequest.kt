package com.joshtalks.joshskills.groups.model

import com.google.gson.annotations.SerializedName
import com.joshtalks.joshskills.groups.constants.OPENED_GROUP

class GroupRequest(
    @field:SerializedName("mentor_id")
    val mentorId: String,

    @field:SerializedName("group_id")
    val groupId: String,

    @field:SerializedName("allow")
    val allow: Boolean = true,

    @field:SerializedName("type")
    val groupType: String = OPENED_GROUP
)
