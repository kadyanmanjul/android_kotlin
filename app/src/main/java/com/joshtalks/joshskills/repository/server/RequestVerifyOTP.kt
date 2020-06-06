package com.joshtalks.joshskills.repository.server

import com.google.gson.annotations.SerializedName

data class RequestVerifyOTP(
    @SerializedName("instance_id") val instanceId: String,
    @SerializedName("country_code") val countryCode: String,
    @SerializedName("mobile") val mobile: String,
    @SerializedName("otp") val otp: String
)