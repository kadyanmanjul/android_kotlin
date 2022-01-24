package com.joshtalks.joshskills.quizgame.ui.data.model


import com.google.gson.annotations.SerializedName

data class Choice(
    @SerializedName("id") var id:String?=null,
    @SerializedName("questions") var questions: String? = null,
    @SerializedName("choice")  var choice: String? = null,
    @SerializedName("is_correct") var isCorrect: Boolean? = null,
    @SerializedName("choice_data") var choiceData : String? = null
)