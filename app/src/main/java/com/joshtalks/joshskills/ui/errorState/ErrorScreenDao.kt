package com.joshtalks.joshskills.ui.errorState

import androidx.room.*
import com.google.gson.annotations.SerializedName

data class ErrorScreen(
    @SerializedName("api_code")
    val apiErrorCode: String,

    @SerializedName("payload")
    val payload:String,

    @SerializedName("exception")
    val exception:String
)