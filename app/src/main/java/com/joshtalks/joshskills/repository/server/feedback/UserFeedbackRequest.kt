package com.joshtalks.joshskills.repository.server.feedback


import com.google.gson.annotations.SerializedName

data class UserFeedbackRequest(
    @SerializedName("mentor")
    val mentor: String,
    @SerializedName("question")
    val question: String,
    @SerializedName("rating")
    val rating: Float,
    @SerializedName("keywords")
    val keywords: String,
    @SerializedName("text")
    val text: String
)