package com.joshtalks.joshskills.ui.group.model

import com.google.gson.annotations.SerializedName

data class EditGroupRequest(

    @field:SerializedName("group_id")
    val groupId: String,

    @field:SerializedName("group_name")
    val groupName: String,

    @field:SerializedName("group_icon")
    var groupIcon: String,

    @Transient
    var isImageChanged: Boolean
)