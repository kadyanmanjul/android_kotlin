package com.joshtalks.joshskills.common.ui.voip.new_arch.ui.report.model

import com.google.gson.annotations.SerializedName

data class VoipOptionModel(
    @SerializedName("created")
    val created: String,
    @SerializedName("id")
    val id: Int,
    @SerializedName("modified")
    val modified: String,
    @SerializedName("option")
    val option: String,
    @SerializedName("type")
    val type: String
)