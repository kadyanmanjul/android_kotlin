package com.joshtalks.joshskills.repository.server


import com.google.gson.annotations.SerializedName
import com.joshtalks.joshskills.core.Utils

data class TrueCallerLoginRequest(
    @SerializedName("payload")
    val payload: String,
    @SerializedName("signature")
    val signature: String,
    @SerializedName("signature_algo")
    val signatureAlgo: String,
    @SerializedName("instance_id")
    val instanceID: String,
    @SerializedName("device_id")
    val deviceId: String = Utils.getDeviceId()
)
