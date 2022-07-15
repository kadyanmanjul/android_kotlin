package com.joshtalks.badebhaiya.core.models

import android.os.Build
import com.google.gson.annotations.SerializedName
import com.joshtalks.badebhaiya.BuildConfig
import com.joshtalks.badebhaiya.repository.model.User
import com.joshtalks.badebhaiya.utils.Utils

data class FormResponse(
    @SerializedName("user")
    internal val user: String,
    @SerializedName("response")
    internal val msg:String,
    @SerializedName("room")
    internal val room:Int
)


data class FormRequest(
    @SerializedName("user")
    internal val user: String,
    @SerializedName("request_submitted")
    internal val msg:String,
    @SerializedName("requested_speaker")
    internal val speaker:String
)