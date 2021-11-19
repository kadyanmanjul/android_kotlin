package com.joshtalks.joshskills.quizgame.ui.data.model

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize

data class SaveRoomDetails (
    @SerializedName("room_id") var roomId:String? = null,
    @SerializedName("team_id") var teamId:String ? =null,
    @SerializedName("team_score") var teamScore:String ?=null,
    @SerializedName("winner_team_status") var winnerTeamStatus:Boolean?=null,
    @SerializedName("call_time") var callTime:String?=null
)
