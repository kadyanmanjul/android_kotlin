package com.joshtalks.joshskills.premium.repository.server.onboarding

import com.google.gson.annotations.SerializedName

data class OnboardingCourseData(
    @SerializedName("title")
    val title: String,

    @SerializedName("course_info_1")
    val courseInfo1: String,

    @SerializedName("course_info_2")
    val courseInfo2: String,

    @SerializedName("course_info_3")
    val courseInfo3: String,
)

data class SpecificOnboardingCourseData(
    @SerializedName("course_id")
    val courseId: String,

    @SerializedName("plan_id")
    val planId: String
)