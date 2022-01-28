package com.joshtalks.joshskills.quizgame.ui.data.model

import com.google.gson.annotations.SerializedName

class AgoraFromToken(
    @SerializedName("from_mentor_id")
    var fromMentorId: String? = null,

    @SerializedName("user_id")
    var userId: String? = null
)