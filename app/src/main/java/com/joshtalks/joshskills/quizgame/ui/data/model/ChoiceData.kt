package com.joshtalks.joshskills.quizgame.ui.data.model

import com.google.gson.annotations.SerializedName

data class ChoiceData(
    @SerializedName("choice_id") var choiceId: Int? = null,
    @SerializedName("is_correct") var isCorrect: String? = null,
    @SerializedName("marked_by_team_id") var markedByTeamId: String? = null,
    @SerializedName("choice_data") var choiceData: String?=null
)