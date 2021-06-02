package com.joshtalks.joshskills.conversationRoom.model

import com.google.gson.annotations.SerializedName

data class JoinConversionRoomRequest(
    @SerializedName("mentor_id") val mentorId: String,
    @SerializedName("room_id") val roomId: Int
)
