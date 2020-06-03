package com.joshtalks.joshskills.repository.server.signup


import com.google.gson.annotations.SerializedName

data class RequestUserVerification(
    @SerializedName("instance_id")
    val instanceId: String,
    @SerializedName("country_code")
    val countryCode: String,
    @SerializedName("mobile")
    val mobile: String
)