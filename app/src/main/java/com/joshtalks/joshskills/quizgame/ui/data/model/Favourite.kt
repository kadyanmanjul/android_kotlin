package com.joshtalks.joshskills.quizgame.ui.data.model

import com.google.gson.annotations.SerializedName

data class Favourite(
    @SerializedName("uuid")
    var uuid: String? = null,
    @SerializedName("name")
    var name: String? = null,
    @SerializedName("image_url")
    var image: String? = null
)