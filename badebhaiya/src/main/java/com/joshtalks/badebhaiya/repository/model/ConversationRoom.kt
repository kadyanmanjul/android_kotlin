package com.joshtalks.badebhaiya.repository.model

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize

data class ConversationRoomRequest(
    @SerializedName("user_id")
    val userId: String?,

    @SerializedName("room_id")
    val roomId: Int? = null,

    @SerializedName("topic")
    val topic: String? = null
)
@Parcelize
data class ConversationRoomResponse(
    @SerializedName("room_id")
    val roomId: Int,

    @SerializedName("moderator_id")
    val moderatorId: Int,

    @SerializedName("token")
    val token: String?,

    @SerializedName("channel_name")
    val channelName: String?,

    @SerializedName("uid")
    val uid: Int,

    @SerializedName("pubnub_token")
    val pubnubToken: String?
) : Parcelable