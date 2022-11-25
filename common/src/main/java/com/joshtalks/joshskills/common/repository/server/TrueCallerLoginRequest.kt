package com.joshtalks.joshskills.common.repository.server


import com.google.gson.annotations.SerializedName
import com.joshtalks.joshskills.common.core.Utils

data class TrueCallerLoginRequest(
    @SerializedName("payload")
    val payload: String,
    @SerializedName("signature")
    val signature: String,
    @SerializedName("signature_algo")
    val signatureAlgo: String,
    @SerializedName("gaid")
    val gaid: String,
    @SerializedName("device_id")
    val deviceId: String = Utils.getDeviceId()
)
