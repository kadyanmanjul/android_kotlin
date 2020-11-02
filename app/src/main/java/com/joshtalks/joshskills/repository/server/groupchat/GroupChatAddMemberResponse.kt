package com.joshtalks.joshskills.repository.server.groupchat


import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize

@Parcelize
data class GroupChatAddMemberResponse(

    @SerializedName("auth_token")
    val authToken: String,

    @SerializedName("group_id")
    val groupId: String,

    @SerializedName("user_id")
    val userId: String

) : Parcelable
