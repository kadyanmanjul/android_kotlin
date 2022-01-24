package com.joshtalks.joshskills.quizgame.ui.data.model

import com.google.gson.annotations.SerializedName

class TeamDataDelete (
    @SerializedName("team_id") var teamId:String,
    @SerializedName("user_id") var userId : String
)