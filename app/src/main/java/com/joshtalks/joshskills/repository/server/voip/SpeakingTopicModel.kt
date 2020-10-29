package com.joshtalks.joshskills.repository.server.voip


import com.google.gson.annotations.SerializedName

data class SpeakingTopicModel(
    @SerializedName("id")
    val id: Int,
    @SerializedName("name")
    val topicName: String,
    @SerializedName("duration")
    val duration: Int,
    @SerializedName("already_talked")
    val alreadyTalked: Int

)