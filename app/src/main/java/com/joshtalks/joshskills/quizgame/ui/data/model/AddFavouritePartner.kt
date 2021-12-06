package com.joshtalks.joshskills.quizgame.ui.data.model

import com.google.gson.annotations.SerializedName

data class AddFavouritePartner (
    @SerializedName("from_user")
    var fromUserId:String?=null,

    @SerializedName("to_user")
    var toUserId:String?=null
)