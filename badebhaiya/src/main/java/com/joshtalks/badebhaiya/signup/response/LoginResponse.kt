package com.joshtalks.badebhaiya.signup.response

import com.google.gson.annotations.SerializedName

data class LoginResponse(
    @SerializedName("user_id")
    val userId: String,
    @SerializedName("mobile")
    val mobile: String,
    @SerializedName("token")
    val token: String,
    @SerializedName("is_user_exist")
    val isUserExist : Boolean
)