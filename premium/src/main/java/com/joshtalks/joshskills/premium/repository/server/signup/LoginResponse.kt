package com.joshtalks.joshskills.premium.repository.server.signup


import com.google.gson.annotations.SerializedName
import com.joshtalks.joshskills.premium.core.EMPTY

data class LoginResponse(
    @SerializedName("id")
    val userId: String,
    @SerializedName("mentor_id")
    val mentorId: String,
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
    @SerializedName("is_user_exist")
    val isUserExist: Boolean,
    @SerializedName("last_login_type")
    val lastLoginType: LastLoginType = LastLoginType.NEVER,
)

enum class LastLoginType {
    @SerializedName("NEVER")
    NEVER, // new user (never logged in)

    @SerializedName("VERIFIED_LOGIN")
    VERIFIED_LOGIN, // user who logged in using otp or social media login

    @SerializedName("UNVERIFIED_LOGIN")
    UNVERIFIED_LOGIN //For users who signed up using name
}