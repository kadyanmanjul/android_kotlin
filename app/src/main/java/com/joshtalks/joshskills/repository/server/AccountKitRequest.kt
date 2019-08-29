package com.joshtalks.joshskills.repository.server

import com.google.gson.annotations.SerializedName


data class AccountKitRequest(
    @SerializedName("authorization_code") val authorizationCode: String,
    @SerializedName("device_id") val deviceId: String,
    @SerializedName("client_id") val clientId: String

)
