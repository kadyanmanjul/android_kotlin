package com.joshtalks.joshskills.ui.userprofile.models

import com.google.gson.annotations.SerializedName

data class GroupsList(
    @SerializedName("groups")
    val myGroupsList: ArrayList<GroupInfo>?,
    @SerializedName("label")
    val label: String?
)

data class GroupsHeader(
    @SerializedName("groups_joined")
    val groupsList: GroupsList
)

data class GroupInfo(
    @SerializedName("text")
    val textToShow: String?,
    @SerializedName("minutes")
    val minutesSpoken: Int?,
    @SerializedName("name")
    val groupName: String?,
    @SerializedName("icon")
    val groupIcon: String?,
)