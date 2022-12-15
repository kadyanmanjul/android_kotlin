package com.joshtalks.joshskills.groups.model

import com.google.gson.annotations.SerializedName
import com.joshtalks.joshskills.common.repository.local.entity.groups.GroupsItem

data class GroupListResponse(

    @field:SerializedName("groups")
    val groups: List<GroupsItem?>? = null
)