package com.joshtalks.joshskills.repository.server.voip


import com.google.gson.annotations.SerializedName

data class RequestVoipRating(
    @SerializedName("mentor")
    val mentor: String,
    @SerializedName("plivo_id")
    val plivoId: String,
    @SerializedName("confidence")
    val confidence: Int,
    @SerializedName("grammar")
    val grammar: Int,
    @SerializedName("pronunciation")
    val pronunciation: Int,
    @SerializedName("eagerness")
    val eagerness: Int,
    @SerializedName("respectfulness")
    val respectfulness: Int
)