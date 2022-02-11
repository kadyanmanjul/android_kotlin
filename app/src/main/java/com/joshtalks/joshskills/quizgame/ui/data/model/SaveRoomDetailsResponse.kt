package com.joshtalks.joshskills.quizgame.ui.data.model

import com.google.gson.annotations.SerializedName

class SaveRoomDetailsResponse(
    @SerializedName("message") var message: String,
    @SerializedName("points") var points: Int
)