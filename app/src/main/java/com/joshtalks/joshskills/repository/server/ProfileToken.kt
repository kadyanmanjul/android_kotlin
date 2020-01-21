package com.joshtalks.joshskills.repository.server

import com.google.gson.annotations.SerializedName


data class ProfileToken(
    @SerializedName("access_token") val accessToken: String,
    @SerializedName("expires_in") val expiresIn: Int,
    @SerializedName("expires_at") val expiresAt: String,
    @SerializedName("token_type") val refreshToken: String,
    @SerializedName("refresh_token") val tokenType: String,
    @SerializedName("scope") val scope: String
)