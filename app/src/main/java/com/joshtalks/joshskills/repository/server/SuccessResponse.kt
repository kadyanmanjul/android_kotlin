package com.joshtalks.joshskills.repository.server


import com.google.gson.annotations.SerializedName

data class SuccessResponse(
    @SerializedName("Success")
    val success: Boolean
)