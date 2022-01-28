package com.joshtalks.joshskills.quizgame.ui.data.model

import com.google.gson.annotations.SerializedName

data class QuestionResponse(@SerializedName("data") var que:List<Question>)

data class Question(
    @SerializedName("id") var id: String? = null,
    @SerializedName("question") var question: String? = null,
    @SerializedName("choices") var choices: List<Choice>? = null
)