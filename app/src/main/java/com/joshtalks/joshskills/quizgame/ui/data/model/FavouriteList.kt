package com.joshtalks.joshskills.quizgame.ui.data.model

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.gson.annotations.SerializedName

data class FavouriteList(
    @SerializedName("data")
    var data: ArrayList<Favourite>? = null
)
