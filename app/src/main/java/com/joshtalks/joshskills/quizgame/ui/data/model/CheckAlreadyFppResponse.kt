package com.joshtalks.joshskills.quizgame.ui.data.model

import com.google.gson.annotations.SerializedName

class CheckAlreadyFppResponse(
    @SerializedName("message") var message: String,
    @SerializedName("already_fpp") var alreadyFpp: String
)