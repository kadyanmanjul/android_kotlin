package com.joshtalks.joshskills.repository.server.course_detail


import com.google.gson.annotations.SerializedName

data class CourseStructure(
    @SerializedName("cDetail")
    val cDetail: List<String>,
    @SerializedName("id")
    val id: Int,
    @SerializedName("title")
    val title: String
)