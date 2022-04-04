package com.joshtalks.badebhaiya.liveroom.model

import com.google.gson.annotations.SerializedName

data class ConversionLiveRoomExitResponse(
    @SerializedName("success")val success: Boolean? = null,
    @SerializedName("message")val message: String
)