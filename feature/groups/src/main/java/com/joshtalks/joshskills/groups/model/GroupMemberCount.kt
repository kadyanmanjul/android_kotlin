package com.joshtalks.joshskills.groups.model

import com.google.gson.annotations.SerializedName

data class GroupMemberCount(

    @field:SerializedName("members")
    val memberCount: Int,

    @field:SerializedName("online")
    val onlineCount: Int
)