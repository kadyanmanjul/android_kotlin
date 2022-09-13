package com.joshtalks.joshskills.ui.payment.new_buy_page_layout.model

import com.google.gson.annotations.SerializedName

data class RatingAndReviews(@SerializedName("reviews") val reviews: List<RatingAndReviewsList>)

data class RatingAndReviewsList(
    @SerializedName("created__date") var createdDate: String,
    @SerializedName("rating") val rating: Float,
    @SerializedName("username") val userName: String,
    @SerializedName("description") val description: String
)