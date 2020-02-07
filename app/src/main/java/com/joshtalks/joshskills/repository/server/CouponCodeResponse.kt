package com.joshtalks.joshskills.repository.server


import com.google.gson.annotations.SerializedName

data class CouponCodeResponse(
    @SerializedName("code")
    val code: String,
    @SerializedName("mentor")
    val mentor: String
)