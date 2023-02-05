package com.joshtalks.joshskills.premium.ui.payment.new_buy_page_layout.model

import com.google.gson.annotations.SerializedName

data class ReviewsListResponse(
    @SerializedName("reviews")
    val reviews: List<ReviewItem>
)

data class ReviewItem(
    @SerializedName("id") val id: Int,
    @SerializedName("created__date") var createdDate: String,
    @SerializedName("rating") val rating: Float,
    @SerializedName("username") val userName: String,
    @SerializedName("description") val description: String
)