package com.joshtalks.joshskills.quizgame.ui.data.model

import com.google.gson.annotations.SerializedName

data class FavouriteList(
    @SerializedName("data")
    var data: ArrayList<Favourite>? = null
)
