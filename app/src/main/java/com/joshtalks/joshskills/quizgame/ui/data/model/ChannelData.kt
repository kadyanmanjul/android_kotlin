package com.joshtalks.joshskills.quizgame.ui.data.model

import com.google.gson.annotations.SerializedName

class ChannelData(
    @SerializedName("token")
    var token:String,
    @SerializedName("channel_name")
    var channelName:String,
    @SerializedName("uid")
    var userUid:String
)