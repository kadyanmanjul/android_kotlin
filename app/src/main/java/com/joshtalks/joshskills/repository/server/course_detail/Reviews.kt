package com.joshtalks.joshskills.repository.server.course_detail


import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize

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
