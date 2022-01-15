package com.joshtalks.joshskills.quizgame.ui.data.model

import com.google.gson.annotations.SerializedName

data class SaveRoomDetails (
    @SerializedName("room_id") var roomId:String? = null,
    @SerializedName("team_id") var teamId:String ? =null,
    @SerializedName("team_score") var teamScore:String ?=null,
    @SerializedName("winner_team_status") var winnerTeamStatus:Boolean?=null,
    @SerializedName("call_time") var callTime:String?=null,
    @SerializedName("user_id") var userId:String?=null
)
