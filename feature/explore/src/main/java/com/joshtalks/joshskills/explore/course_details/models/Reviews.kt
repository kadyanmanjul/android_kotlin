package com.joshtalks.joshskills.explore.course_details.models


import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class Reviews(
    @SerializedName("title")
    val title: String,

    @SerializedName("value")
    val value: Double,

    @SerializedName("ratings")
    val ratingList: List<Rating>,

    @SerializedName("reviews")
    val reviews: List<Review>

) : Parcelable