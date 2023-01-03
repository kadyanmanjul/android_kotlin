package com.joshtalks.joshskills.explore.course_details.models


import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import com.joshtalks.joshskills.common.repository.local.entity.VideoType
import kotlinx.android.parcel.Parcelize

@Parcelize
data class DemoLesson(
    @SerializedName("title")
    val title: String,
    @SerializedName("demo_video")
    val video: VideoType?
) : Parcelable


