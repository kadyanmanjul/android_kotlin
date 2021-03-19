package com.joshtalks.joshskills.repository.server.course_detail.demoCourseDetails


import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize

@Parcelize
data class SuperStarResponse(
    @SerializedName("title")
    val title: String?,
    @SerializedName("feedback_list")
    val feedback_list: List<Feedback>?
) : Parcelable

@Parcelize
data class Feedback(
    @SerializedName("feedback")
    val feedback: String?,
    @SerializedName("name")
    val name: String?,
    @SerializedName("photo_url")
    val photoUrl: String?,
    @SerializedName("place")
    val place: String?,
    @SerializedName("video_url")
    val videoUrl: String?
) : Parcelable
