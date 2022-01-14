package com.joshtalks.joshskills.quizgame.ui.data.model

import com.google.gson.annotations.SerializedName

class AgoraToToken(
    @SerializedName("to_mentor_id")
    var toMentorId: String? = null,

    @SerializedName("channel_name")
    var channelName: String? = null,

    @SerializedName("user_id")
    var userId: String? = null
)