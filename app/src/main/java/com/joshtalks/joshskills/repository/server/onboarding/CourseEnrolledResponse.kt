package com.joshtalks.joshskills.repository.server.onboarding


import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize

data class CourseEnrolledResponse(
    @SerializedName("content_list")
    val contentList: List<CourseContent>,
    @SerializedName("description")
    val description: String,
    @SerializedName("test_ids")
    val testIds: List<Int>,
    @SerializedName("text")
    val text: String
)

@Parcelize
data class CourseContent(
    @SerializedName("heading")
    val heading: String,
    @SerializedName("text")
    val text: String,
    @SerializedName("thumbnail")
    val thumbnail: String
) : Parcelable
