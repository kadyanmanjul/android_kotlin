package com.joshtalks.joshskills.repository.server


import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import com.joshtalks.joshskills.core.EMPTY
import kotlinx.android.parcel.Parcelize


@Parcelize
data class CourseEnrolled(
    @SerializedName("name")
    val courseName: String = EMPTY,
    @SerializedName("image")
    val courseImage: String = EMPTY,
    @SerializedName("total_enrolled")
    val noOfStudents: Int = 0,
    @SerializedName("sort_order")
    val sortOrder: Int = 0,
) : Parcelable
