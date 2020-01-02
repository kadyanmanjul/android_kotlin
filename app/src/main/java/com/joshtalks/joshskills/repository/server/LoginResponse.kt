package com.joshtalks.joshskills.repository.server


import com.google.gson.annotations.SerializedName

data class LoginResponse(
    @SerializedName("first_name")
    val firstName: String,
    @SerializedName("id")
    val userId: String,
    @SerializedName("mentor_id")
    val mentorId: String,
    @SerializedName("mobile")
    val mobile: String,
    @SerializedName("token")
    val token: String
)