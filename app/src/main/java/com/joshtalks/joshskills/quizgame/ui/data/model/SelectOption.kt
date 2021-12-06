package com.joshtalks.joshskills.quizgame.ui.data.model

import com.google.gson.annotations.SerializedName

class SelectOption(
    @SerializedName("room_id") var roomId:String,
    @SerializedName("question_id") var  questionId:String,
    @SerializedName("choice_id") var choiceId:String,
    @SerializedName("team_id") var teamId:String
)