package com.joshtalks.joshskills.repository.server.course_detail


import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class Feedback(
    @SerializedName("name")
    val name: String,

    @SerializedName("short_description")
    val shortDescription: String,

    @SerializedName("thumbnail_url")
    val thumbnailUrl: String?,

    @SerializedName("video_url")
    val videoUrl: String?

) : Parcelable
