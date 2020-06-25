package com.joshtalks.joshskills.repository.server.course_detail


import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize

@Parcelize
data class DemoLesson(
    @SerializedName("title")
    val title: String,

    @SerializedName("demo_video_url")
    val videoUrl: String,

    @SerializedName("video_thumbnail")
    val thumbnailUrl: String

) : Parcelable
