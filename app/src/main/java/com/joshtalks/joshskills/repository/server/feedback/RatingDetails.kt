package com.joshtalks.joshskills.repository.server.feedback


import com.google.gson.annotations.SerializedName

data class RatingDetails(
    @SerializedName("info_text")
    val infoText: String,
    @SerializedName("keywords")
    val keywords: String,
    @SerializedName("rating")
    val rating: Int
)