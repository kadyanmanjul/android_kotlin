package com.joshtalks.joshskills.ui.lesson.conversationRoom.model

import com.google.gson.annotations.SerializedName

data class ConversionLiveRoomExitResponse(
    @SerializedName("success")val success: Boolean? = null,
    @SerializedName("message")val message: String
)