package com.joshtalks.badebhaiya.repository.model

import com.google.gson.annotations.SerializedName

data class ConversationRoomRequest(
    @SerializedName("user_id")
    val userId: String?,

    @SerializedName("room_id")
    val roomId: String? = null,

    @SerializedName("topic")
    val topic: String? = null
)

data class ConversationRoomResponse(
    @SerializedName("room_id")
    val roomId: Int,

    @SerializedName("token")
    val token: String?,

    @SerializedName("channel_name")
    val channelName: String?,

    @SerializedName("uid")
    val uid: Int,

    @SerializedName("pubnub_token")
    val pubnubToken: String?
)