package com.joshtalks.joshskills.repository.server


import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import com.joshtalks.joshskills.core.EMPTY
import kotlinx.android.parcel.Parcelize


@Parcelize
data class EnrolledCoursesList(
    @SerializedName("label")
    val label: String = EMPTY,
    @SerializedName("courses")
    val courses: List<CourseEnrolled> = listOf()
) : Parcelable
