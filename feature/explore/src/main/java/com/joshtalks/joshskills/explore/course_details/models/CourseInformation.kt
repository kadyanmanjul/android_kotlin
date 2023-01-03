package com.joshtalks.joshskills.explore.course_details.models


import com.google.gson.annotations.SerializedName

data class CourseInformation(
    @SerializedName("title")
    val title: String,
    @SerializedName("desc")
    val desc: String,
    @SerializedName("is_list")
    val isList: Boolean,
    @SerializedName("id")
    var id: Int

)