package com.joshtalks.joshskills.premium.repository.server.feedback


import com.google.gson.annotations.SerializedName

data class SubmittedData(
    @SerializedName("id")
    val id: Int,
    @SerializedName("keywords")
    val keywords: String,
    @SerializedName("mentor")
    val mentor: String,
    @SerializedName("question")
    val question: Int,
    @SerializedName("rating")
    val rating: Double,
    @SerializedName("text")
    val text: String
)