package com.joshtalks.joshskills.repository.server.onboarding

import com.google.gson.annotations.SerializedName

data class OnboardingCourseData(
    @SerializedName("title")
    val title: String = "Spoken English Course",

    @SerializedName("course_info_1")
    val courseInfo1: String = "90 Day Course",

    @SerializedName("course_info_2")
    val courseInfo2: String = "Beginner to Advanced",

    @SerializedName("course_info_3")
    val courseInfo3: String = "English Speaking का माहौल",
)