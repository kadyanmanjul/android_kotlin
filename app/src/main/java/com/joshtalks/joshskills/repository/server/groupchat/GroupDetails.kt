package com.joshtalks.joshskills.repository.server.groupchat


import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize

@Parcelize
data class GroupDetails(

    @SerializedName("auth_token")
    val authToken: String,

    @SerializedName("group_id")
    val groupId: String,

    @SerializedName("user_id")
    val userId: String,

    @SerializedName("group_description")
    val groupDescription: String?,

    @SerializedName("group_icon_url")
    val groupIconUrl: String?,

    @SerializedName("group_member_count")
    val groupMemberCount: Int,

    @SerializedName("group_name")
    val groupName: String,

    @SerializedName("group_owner_uid")
    val groupOwnerUid: String,

    @SerializedName("group_password")
    val groupPassword: String?,

    @SerializedName("group_type")
    val groupType: String

) : Parcelable
