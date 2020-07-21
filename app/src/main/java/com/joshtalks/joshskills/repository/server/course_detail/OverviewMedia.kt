package com.joshtalks.joshskills.repository.server.course_detail


import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import com.joshtalks.joshskills.repository.local.entity.VideoType
import kotlinx.android.parcel.Parcelize

@Parcelize
data class OverviewMedia(
    @SerializedName("sort_order")
    val sortOrder: Int,

    @SerializedName("type")
    val type: OverviewMediaType,

    @SerializedName("url")
    val url: String?,

    @SerializedName("text")
    val text: String?,

    @SerializedName("video_thumbnail")
    val thumbnailUrl: String?,

    @SerializedName("video")
    val video: VideoType?

) : Parcelable

enum class OverviewMediaType(type: String) {
    IMAGE("IMAGE"),
    VIDEO("VIDEO"),
    ICON("ICON")
}
