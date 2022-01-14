package com.joshtalks.joshskills.quizgame.ui.data.model

import com.google.gson.annotations.SerializedName

class SaveCallDurationRoomData(
    @SerializedName("room_id") var roomId: String,
    @SerializedName("user_id") var userId: String,
    @SerializedName("team_id") var teamId: String,
    @SerializedName("call_duration") var duration: String,
)