package com.joshtalks.joshskills.quizgame.ui.data.model

import com.google.gson.annotations.SerializedName

class AgoraToTokenResponse(
@SerializedName("token")
var token:String?=null,

@SerializedName("uid")
var uid:String?=null,

@SerializedName("channel_name")
var channelName :String?=null,

@SerializedName("message")
var message : String?=null
)