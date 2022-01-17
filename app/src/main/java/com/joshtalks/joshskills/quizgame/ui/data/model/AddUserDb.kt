package com.joshtalks.joshskills.quizgame.ui.data.model

import com.google.gson.annotations.SerializedName

data class AddUserDb(
    @SerializedName("user_id") var userUid:String?,
    @SerializedName("username") var name:String?,
    @SerializedName("image_url") var imageUrl:String?
)