package com.joshtalks.joshskills.quizgame.ui.data.model

import com.google.gson.annotations.SerializedName

class QuestionRequest (
    @SerializedName("no_of_questions") var noOfQuestion:String,
    @SerializedName("room_id") var roomid:String
)