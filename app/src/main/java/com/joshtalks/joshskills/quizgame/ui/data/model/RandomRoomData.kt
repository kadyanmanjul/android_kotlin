package com.joshtalks.joshskills.quizgame.ui.data.model

import com.google.gson.annotations.SerializedName

class RandomRoomData (
    @SerializedName ("room_id") var roomId:String,
    @SerializedName("user_id") var userId : String
)