package com.joshtalks.badebhaiya.signup.request

import com.google.gson.annotations.SerializedName
import com.truecaller.android.sdk.TruecallerSDK

data class VerifyOTPRequest(
    @SerializedName("country_code") val countryCode: String,
    @SerializedName("mobile") val mobile: String,
    @SerializedName("otp") val otp: String,
    @SerializedName("device_id") val deviceId: String,
    @SerializedName("is_tc_installed") val isTcInstalled: Boolean = TruecallerSDK.getInstance().isUsable,
)