package com.joshtalks.joshskills.repository.server.course_overview

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName


data class CourseOverviewResponse(

    @SerializedName("title")
    @Expose
    var title: String,
    @SerializedName("total_count")
    @Expose
    var totalCount: Int,
    @SerializedName("data")
    @Expose
    var data: List<CourseOverviewItem>
)

data class CourseOverviewItem(

    @SerializedName("lesson_id")
    @Expose
    var lessonId: Int,
    @SerializedName("lesson_name")
    @Expose
    var lessonName: String,
    @SerializedName("status")
    @Expose
    var status: String,
    @SerializedName("grammar_percentage")
    @Expose
    var grammarPercentage: String,
    @SerializedName("rp_percentage")
    @Expose
    var rpPercentageval: String,
    @SerializedName("vp_percentage")
    @Expose
    var vpPercentage: String,
    @SerializedName("speaking_percentage")
    @Expose
    var speakingPercentage: String
)