package com.joshtalks.joshskills.quizgame.ui.data.model

import com.google.gson.annotations.SerializedName

data class CallDurationResponse(
    @SerializedName("message") val message: String,
    @SerializedName("points") var points: String
)