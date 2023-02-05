package com.joshtalks.joshskills.premium.repository.server.signup


import com.google.gson.annotations.SerializedName

data class RequestUserVerification(
    @SerializedName("gaid")
    val gaid: String,
    @SerializedName("country_code")
    val countryCode: String,
    @SerializedName("mobile")
    val mobile: String
)