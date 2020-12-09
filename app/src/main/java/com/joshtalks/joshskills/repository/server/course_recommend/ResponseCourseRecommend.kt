package com.joshtalks.joshskills.repository.server.course_recommend


import com.google.gson.annotations.SerializedName
import com.joshtalks.joshskills.repository.local.model.ExploreCardType
import com.joshtalks.joshskills.repository.server.CourseExploreModel

data class ResponseCourseRecommend(
    @SerializedName("id")
    val id: Int,
    @SerializedName("name")
    val languageName: String,
    @SerializedName("sort_order")
    val sortOrder: Int,
    @SerializedName("explore_type")
    var exploreCardType: ExploreCardType = ExploreCardType.NORMAL,
    @SerializedName("segment_list")
    val segmentList: List<Segment> = emptyList(),
    @SerializedName("test_list")
    val courseList: List<CourseExploreModel> = emptyList()
)