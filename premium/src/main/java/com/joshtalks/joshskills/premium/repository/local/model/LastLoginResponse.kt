package com.joshtalks.joshskills.premium.repository.local.model

import com.google.gson.annotations.SerializedName

data class LastLoginResponse(
    @SerializedName("success")
    val success: Boolean = true,
    @SerializedName("is_latest_login_device")
    val isLatestLoginDevice: Boolean = true,
)
