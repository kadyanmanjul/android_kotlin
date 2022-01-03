package com.joshtalks.joshskills.quizgame.ui.data.model

import com.google.gson.annotations.SerializedName

class SaveCallDuration (
    @SerializedName("team_id") var teamId : String,
    @SerializedName("call_duration") var callDuration : String,
    @SerializedName("user_id") var userId :String
)