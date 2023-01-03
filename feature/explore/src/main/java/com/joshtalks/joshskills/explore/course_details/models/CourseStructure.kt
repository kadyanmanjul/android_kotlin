package com.joshtalks.joshskills.explore.course_details.models


import com.google.gson.annotations.SerializedName

data class CourseStructure(
    @SerializedName("cDetail")
    val cDetail: List<String>,
    @SerializedName("id")
    val id: Int,
    @SerializedName("title")
    val title: String
)