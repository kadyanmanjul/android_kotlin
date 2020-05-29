package com.joshtalks.joshskills.repository.server.signup


import com.google.gson.annotations.SerializedName
import com.joshtalks.joshskills.core.EMPTY

data class LoginResponse(
    @SerializedName("id")
    val userId: String,
    @SerializedName("mentor_id")
    val mentorId: String,
    @SerializedName("instance_id")
    val instance_id: String,
    @SerializedName("mobile")
    val mobile: String,
    @SerializedName("token")
    val token: String,
    @SerializedName("referral_code")
    val referralCode: String = EMPTY,
    @SerializedName("new_user")
    val newUser: Boolean,
    @SerializedName("created_source")
    val createdSource: String
)