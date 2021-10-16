package com.joshtalks.joshskills.conversationRoom.model

import com.google.gson.annotations.SerializedName

data class EnterExitConversionRoomRequest(
    @SerializedName("mentor_id") val mentorId: String
)
