package com.joshtalks.joshskills.premium.repository.server


import com.google.gson.annotations.SerializedName

data class SuccessResponse(
    @SerializedName("Success")
    val success: Boolean
)