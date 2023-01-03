package com.joshtalks.joshskills.explore.course_details.models


import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

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
    val viewerText: String,

    @SerializedName("top_icon_url")
    val topIconUrl: String?

) : Parcelable
