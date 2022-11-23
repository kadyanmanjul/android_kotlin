package com.joshtalks.joshskills.ui.inbox.adapter

import com.google.gson.annotations.SerializedName

data class InboxRecommendedCourse (
    @SerializedName("id") var id:Int,
    @SerializedName("course_name") var courseName:String,
    @SerializedName("course_icon") var courseIcon:String
)