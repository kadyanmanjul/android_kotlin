package com.joshtalks.joshskills.quizgame.ui.data.model

import com.google.gson.annotations.SerializedName

class CheckAlreadyFpp(
    @SerializedName("user_id") var userId: String,
    @SerializedName("team_id") var teamId: String
)