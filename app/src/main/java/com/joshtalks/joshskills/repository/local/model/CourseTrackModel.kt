package com.joshtalks.joshskills.repository.local.model


import com.google.gson.annotations.SerializedName
import com.joshtalks.joshskills.core.EMPTY
import java.text.SimpleDateFormat
import java.util.*

data class CourseTrackModel(
    @SerializedName("course_buy")
    val courseBuy: ArrayList<CourseDetailModel> = arrayListOf(),
    @SerializedName("course_watch")
    val courseWatch: ArrayList<CourseDetailModel> = arrayListOf(),
    @SerializedName("course_buy_initialize")
    val courseBuyInitialize: ArrayList<CourseDetailModel> = arrayListOf(),
    @SerializedName("mentor_id")
    var mentorId: String = EMPTY,
    @SerializedName("unique_id")
    var uniqueId: String = EMPTY,
    @SerializedName("mobile")
    var mobileNumber: String = EMPTY
)

data class CourseDetailModel(
    @SerializedName("course_id")
    val courseId: String = EMPTY,
    @SerializedName("course_name")
    val courseName: String = EMPTY,
    @SerializedName("created_time")
    val createdTime: String = SimpleDateFormat("yyyy/MM/dd HH:mm:SS", Locale.ENGLISH).format(Date())
)