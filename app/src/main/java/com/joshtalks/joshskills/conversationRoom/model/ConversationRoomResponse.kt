package com.joshtalks.joshskills.conversationRoom.model

import com.google.gson.annotations.SerializedName

data class ConversationRoomResponse(
    @SerializedName("channel_name") val channelName: String? = null,
    @SerializedName("uid") val uid: Int? = null,
    @SerializedName("token") val token: String? = null,
    @SerializedName("room_id") val roomId: Int? = null
)

