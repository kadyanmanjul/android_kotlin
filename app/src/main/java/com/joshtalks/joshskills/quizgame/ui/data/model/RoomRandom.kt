package com.joshtalks.joshskills.quizgame.ui.data.model

import com.google.gson.annotations.SerializedName

class RoomRandom(
    @SerializedName("user_ids") var listOfUsers: ArrayList<String>,
    @SerializedName("user_id") var userId: String? = null
)