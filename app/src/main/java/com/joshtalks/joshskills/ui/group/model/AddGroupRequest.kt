package com.joshtalks.joshskills.ui.group.model

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize

@Parcelize
data class AddGroupRequest(

    @field:SerializedName("mentor_id")
    val mentorId: String,

    @field:SerializedName("group_name")
    val groupName: String,

    @field:SerializedName("group_icon")
    var groupIcon: String,

    @field:SerializedName("group_type")
    val groupType: String
) : Parcelable
