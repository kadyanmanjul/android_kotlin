package com.joshtalks.joshskills.quizgame.ui.data.model

import com.google.gson.annotations.SerializedName

class SearchRandomResponse (
    @SerializedName("message") var message:String,
    @SerializedName("data") var data:ArrayList<String>
)