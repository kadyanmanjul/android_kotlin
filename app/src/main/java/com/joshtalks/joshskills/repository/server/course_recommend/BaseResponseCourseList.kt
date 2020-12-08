package com.joshtalks.joshskills.repository.server.course_recommend

import com.google.gson.annotations.SerializedName

class BaseResponseCourseList {
    @SerializedName("language_list")
    val languageSpecificCourses: List<ResponseCourseRecommend> = emptyList()
}