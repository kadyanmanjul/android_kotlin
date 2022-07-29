package com.joshtalks.joshskills.base.model.groups

import com.google.gson.annotations.SerializedName

data class GroupMemberCount(

    @field:SerializedName("members")
    val memberCount: Int,

    @field:SerializedName("online")
    val onlineCount: Int
)