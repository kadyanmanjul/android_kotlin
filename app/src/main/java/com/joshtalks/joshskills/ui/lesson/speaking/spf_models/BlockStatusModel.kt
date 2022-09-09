package com.joshtalks.joshskills.ui.lesson.speaking.spf_models

import com.google.gson.annotations.SerializedName

data class BlockStatusModel(
    @SerializedName("time_left")
    val duration : Int,
    @SerializedName("calls_left")
    val callsLeft : Int,
    @SerializedName("block_reason")
    val reason : String,
    @SerializedName("timestamp")
    var timestamp : Long,
    )