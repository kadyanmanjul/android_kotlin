package com.joshtalks.joshskills.repository.server


import com.google.gson.annotations.SerializedName

data class TrueCallerLoginRequest(
    @SerializedName("payload")
    val payload: String,
    @SerializedName("signature")
    val signature: String,
    @SerializedName("signature_algo")
    val signatureAlgo: String,
    @SerializedName("instance_id")
    val instanceID: String
)