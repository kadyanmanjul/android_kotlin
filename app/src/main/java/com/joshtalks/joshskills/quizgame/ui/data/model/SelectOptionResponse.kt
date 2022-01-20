package com.joshtalks.joshskills.quizgame.ui.data.model

import com.google.gson.annotations.SerializedName

data class SelectOptionResponse(
    @SerializedName("message") var message: String? = null,
    @SerializedName("choice") var choiceData: List<ChoiceData>?=null,
    @SerializedName("opponent_team") var opponentTeamId: String? = null
)

