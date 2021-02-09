package com.joshtalks.joshskills.repository.server.voip

import com.google.gson.annotations.SerializedName

data class RequestUserLocation(
    @SerializedName("channel_name")
    var channelName: String,
    @SerializedName("latitude")
    var latitude: Double,
    @SerializedName("longitude")
    var longitude: Double
)