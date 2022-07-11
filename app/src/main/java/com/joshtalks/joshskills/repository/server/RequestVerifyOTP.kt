package com.joshtalks.joshskills.repository.server

import com.google.gson.annotations.SerializedName
import com.joshtalks.joshskills.core.Utils

data class RequestVerifyOTP(
    @SerializedName("gaid") val gaid: String,
    @SerializedName("country_code") val countryCode: String,
    @SerializedName("mobile") val mobile: String,
    @SerializedName("otp") val otp: String,
    @SerializedName("device_id") val deviceId: String = Utils.getDeviceId()
)
