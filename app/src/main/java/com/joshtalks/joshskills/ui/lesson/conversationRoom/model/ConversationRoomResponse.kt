package com.joshtalks.joshskills.ui.lesson.conversationRoom.model

import com.google.gson.annotations.SerializedName

data class ConversationRoomResponse(
    @SerializedName("channel_name") val channelName: String? = null,
    @SerializedName("uid") val uid: Int? = null,
    @SerializedName("token") val token: String? = null,
    @SerializedName("pubnub_token") val pubNubToken: String? = null,
    @SerializedName("room_id") val roomId: Int? = null,
    @SerializedName("message") val message: String? = null,
    @SerializedName("error_code") val errorCode: Int? = null
)

