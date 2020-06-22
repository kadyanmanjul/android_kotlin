package com.joshtalks.joshskills.repository.server.course_detail


import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize

@Parcelize
data class StudentFeedback(
    @SerializedName("title")
    val title: String,

    @SerializedName("feedbacks")
    val feedbacks: List<Feedback>

) : Parcelable
