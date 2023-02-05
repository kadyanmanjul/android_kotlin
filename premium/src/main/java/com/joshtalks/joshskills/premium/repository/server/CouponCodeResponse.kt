package com.joshtalks.joshskills.premium.repository.server


import com.google.gson.annotations.SerializedName

data class CouponCodeResponse(
    @SerializedName("code")
    val code: String,
    @SerializedName("mentor")
    val mentor: String
)