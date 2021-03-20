package com.joshtalks.joshskills.repository.server.course_detail.demoCourseDetails


import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize

@Parcelize
data class DemoLesson2Response(
    @SerializedName("title")
    val title: String?,
    @SerializedName("button_text")
    val button_text: String?,
    @SerializedName("lesson_id")
    val lessonId: Int?
) : Parcelable


