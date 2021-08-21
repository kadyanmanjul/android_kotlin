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
    @SerializedName("is_verified")
    val isVerified: Boolean,
    @SerializedName("created_source")
    val createdSource: String?,
    @SerializedName("engagement_version")
    val engagement_version: EngagementVersion?
)


enum class EngagementVersion(val type: String) {
    V1("V1"), // # Everything Normal(no free trial) (no guest user) (Old Grammar)
    V2("V2"), // # Login and then start Free Trial (no guest user) (Old Grammar)
    V3("V3"), // # Free Trial with Guest User (New Grammar)
    V4("V4"), // # Free Trial with Guest User (Old Grammar)
}