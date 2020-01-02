package com.joshtalks.joshskills.repository.server

import com.google.gson.annotations.SerializedName

data class RequestVerifyOTP(
    @SerializedName("mobile") val mobile: String,
    @SerializedName("otp") val otp: String
)