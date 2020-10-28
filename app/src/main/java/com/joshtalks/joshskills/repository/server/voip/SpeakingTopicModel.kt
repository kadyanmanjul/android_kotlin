package com.joshtalks.joshskills.repository.server.voip


import com.google.gson.annotations.SerializedName

data class SpeakingTopicModel(
    @SerializedName("already_talked")
    val alreadyTalked: Int,
    @SerializedName("duration")
    val duration: Int,
    @SerializedName("id")
    val id: Int,
    @SerializedName("name")
    val name: String
)