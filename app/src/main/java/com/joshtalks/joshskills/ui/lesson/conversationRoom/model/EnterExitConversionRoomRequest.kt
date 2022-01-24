package com.joshtalks.joshskills.ui.lesson.conversationRoom.model

import com.google.gson.annotations.SerializedName

data class EnterExitConversionRoomRequest(
    @SerializedName("mentor_id") val mentorId: String
)
