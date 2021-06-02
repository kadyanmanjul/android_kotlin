package com.joshtalks.joshskills.conversationRoom.model

import com.google.gson.annotations.SerializedName

data class CreateConversionRoomRequest(
    @SerializedName("mentor_id") val mentorId: String,
    @SerializedName("topic") val topic: String
)
