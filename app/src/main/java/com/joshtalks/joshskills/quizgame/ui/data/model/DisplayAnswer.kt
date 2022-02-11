package com.joshtalks.joshskills.quizgame.ui.data.model

import com.google.gson.annotations.SerializedName

class DisplayAnswer(
    @SerializedName("room_id") var roomId: String,
    @SerializedName("question_id") var questionId: String,
    @SerializedName("user_id") var userId: String
)