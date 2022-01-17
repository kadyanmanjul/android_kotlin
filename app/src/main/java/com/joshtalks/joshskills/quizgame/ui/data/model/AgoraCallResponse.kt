package com.joshtalks.joshskills.quizgame.ui.data.model

import com.google.gson.annotations.SerializedName

class AgoraCallResponse (
    @SerializedName("uid") var uid:String?,
    @SerializedName("channel_name") var channelName:String?,
    @SerializedName("call_response") var callResponse:String?
)
