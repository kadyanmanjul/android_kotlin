package com.joshtalks.joshskills.ui.userprofile.models

import com.google.gson.annotations.SerializedName
import com.joshtalks.joshskills.core.EMPTY

data class CourseEnrolled(
    @SerializedName("name")
    val courseName: String = EMPTY,
    @SerializedName("image")
    val courseImage: String = EMPTY,
    @SerializedName("rating")
    val courseRating: String = EMPTY,
    @SerializedName("total_enrolled")
    val noOfStudents: Int = 0,
    @SerializedName("sort_order")
    val sortOrder: Int = 0,
)
data class CourseHeader(
    @SerializedName("course_enrolled")
    val enrolledCoursesList: EnrolledCoursesList
    )

data class EnrolledCoursesList(
    @SerializedName("label")
    val label: String = EMPTY,
    @SerializedName("courses")
    val courses: ArrayList<CourseEnrolled> = arrayListOf()
)