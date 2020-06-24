package com.joshtalks.joshskills.repository.server.course_detail


import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize

@Parcelize
data class CourseOverviewData(
    @SerializedName("name")
    val courseName: String,

    @SerializedName("rating")
    val rating: Double,

    @SerializedName("short_description")
    val shortDescription: String,

    @SerializedName("teacher_name")
    val teacherName: String,

    @SerializedName("media")
    val media: List<OverviewMedia>,

    @SerializedName("viewer_text")
    val viewerText: String

) : Parcelable
