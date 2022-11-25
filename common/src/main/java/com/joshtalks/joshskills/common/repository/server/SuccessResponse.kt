package com.joshtalks.joshskills.common.repository.server


import com.google.gson.annotations.SerializedName

data class SuccessResponse(
    @SerializedName("Success")
    val success: Boolean
)