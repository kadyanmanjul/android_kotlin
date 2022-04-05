package com.joshtalks.badebhaiya.liveroom.model

import com.google.gson.annotations.SerializedName

data class EnterExitConversionRoomRequest(
    @SerializedName("mentor_id") val mentorId: String
)
