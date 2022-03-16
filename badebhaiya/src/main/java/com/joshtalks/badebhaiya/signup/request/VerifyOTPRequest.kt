package com.joshtalks.badebhaiya.signup.request

import com.google.gson.annotations.SerializedName

data class VerifyOTPRequest(
    @SerializedName("country_code") val countryCode: String,
    @SerializedName("mobile") val mobile: String,
    @SerializedName("otp") val otp: String
)