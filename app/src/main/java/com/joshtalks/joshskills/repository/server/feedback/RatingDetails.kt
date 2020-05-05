package com.joshtalks.joshskills.repository.server.feedback


import com.google.gson.annotations.SerializedName

data class RatingDetails(
    @SerializedName("info_text")
    val infoText: String,
    @SerializedName("keywords")
    val keywords: String,
    @SerializedName("rating")
    val rating: Int
) {
    val keywordsList: List<String>
        get() {
            return keywords.split(",")
        }
}

data class RatingModel(
    var label: String,
    var rating: Int,
    var click: Boolean,
    var enable: Boolean
)