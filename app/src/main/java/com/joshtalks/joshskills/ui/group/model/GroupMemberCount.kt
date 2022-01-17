package com.joshtalks.joshskills.ui.group.model

import com.google.gson.annotations.SerializedName

data class GroupMemberCount(

    @field:SerializedName("members")
    val memberCount: Int,

    @field:SerializedName("online")
    val onlineCount: Int
)