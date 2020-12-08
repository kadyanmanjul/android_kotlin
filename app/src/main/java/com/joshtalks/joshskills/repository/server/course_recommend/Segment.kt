package com.joshtalks.joshskills.repository.server.course_recommend

import com.google.gson.annotations.SerializedName
import com.joshtalks.joshskills.repository.server.CourseExploreModel

data class Segment(
    @SerializedName("id")
    val id: Int,
    @SerializedName("name")
    val name: String,
    @SerializedName("sort_order")
    val sortOrder: Int,
    @SerializedName("test_list")
    val courseList: List<CourseExploreModel>
)