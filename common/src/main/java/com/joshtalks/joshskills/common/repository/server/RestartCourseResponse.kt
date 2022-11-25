package com.joshtalks.joshskills.common.repository.server

import com.google.gson.annotations.SerializedName

data class RestartCourseResponse(
    @SerializedName("message")
    val batchChanged :String,
    @SerializedName("success")
    val success :Boolean
)