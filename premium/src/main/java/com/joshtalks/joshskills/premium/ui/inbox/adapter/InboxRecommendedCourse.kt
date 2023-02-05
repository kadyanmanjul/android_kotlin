package com.joshtalks.joshskills.premium.ui.inbox.adapter

import com.google.gson.annotations.SerializedName

data class InboxRecommendedCourse (
    @SerializedName("id") var id:Int,
    @SerializedName("course_name") var courseName:String,
    @SerializedName("course_icon") var courseIcon:String
)