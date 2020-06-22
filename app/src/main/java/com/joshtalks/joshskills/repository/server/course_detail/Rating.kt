package com.joshtalks.joshskills.repository.server.course_detail


import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize

@Parcelize
data class Rating(
    @SerializedName("value")
    val overallRating: Double,

    @SerializedName("value_1")
    val oneStarRatingPercentage: Int,

    @SerializedName("value_2")
    val twoStarRatingPercentage: Int,

    @SerializedName("value_3")
    val threeStarRatingPercentage: Int,

    @SerializedName("value_4")
    val fourStarRatingPercentage: Int,

    @SerializedName("value_5")
    val fiveStarRatingPercentage: Int

) : Parcelable
