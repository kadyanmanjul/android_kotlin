package com.joshtalks.badebhaiya.signup.response

import com.google.gson.annotations.SerializedName

data class OTPResponse(
    @SerializedName("protocol")
    val protocol: String,
    @SerializedName("code")
    val code: Int,
    @SerializedName("message")
    val msg: String,
    @SerializedName("url")
    val url : String
)