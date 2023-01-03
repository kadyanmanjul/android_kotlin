package com.joshtalks.joshskills.explore.course_details.models

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class StudentFeedback(
    @SerializedName("title")
    val title: String,

    @SerializedName("feedbacks")
    val feedbacks: List<VideoFeedback>

) : Parcelable

@Parcelize
data class VideoFeedback(
    @SerializedName("name")
    val name: String,

    @SerializedName("short_description")
    val shortDescription: String,

    @SerializedName("thumbnail_url")
    val thumbnailUrl: String?,

    @SerializedName("video_url")
    val videoUrl: String?

) : Parcelable
