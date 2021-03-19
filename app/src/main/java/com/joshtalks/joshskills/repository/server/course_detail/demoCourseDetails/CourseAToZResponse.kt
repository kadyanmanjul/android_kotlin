package com.joshtalks.joshskills.repository.server.course_detail.demoCourseDetails


import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize

@Parcelize
data class CourseAToZResponse(
    @SerializedName("title")
    val title: String?,
    @SerializedName("description")
    val description: String?
) : Parcelable
