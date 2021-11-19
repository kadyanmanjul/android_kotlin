package com.joshtalks.joshskills.quizgame.ui.data.model

import com.google.gson.annotations.SerializedName

class SelectOptionResponse(
    @SerializedName("message") var message: String? = null,
    //@SerializedName("choice") var choice: ChoiceData,
    @SerializedName("opponent_team") var opponentTeamId: String? = null
)

class ChoiceData(
    @SerializedName("choice_id") var choiceId: Int? = null,
    @SerializedName("is_correct") var isCorrect: String? = null
)
