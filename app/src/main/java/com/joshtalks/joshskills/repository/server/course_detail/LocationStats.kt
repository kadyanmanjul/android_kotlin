package com.joshtalks.joshskills.repository.server.course_detail


import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize

@Parcelize
data class LocationStats(
    @SerializedName("student_text")
    val studentText: String,

    @SerializedName("location_text")
    val locationText: String,

    @SerializedName("bg_images_urls")
    val imageUrls: List<String>,

    @SerializedName("total_users")
    val totalEnrolled: Int

) : Parcelable
