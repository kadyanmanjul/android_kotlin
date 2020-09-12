package com.joshtalks.joshskills.repository.server


import com.google.gson.annotations.SerializedName

data class ActiveUserRequest(
    @SerializedName("mentor") val mentor: String,
    @SerializedName("is_active") val isActive: Boolean
)