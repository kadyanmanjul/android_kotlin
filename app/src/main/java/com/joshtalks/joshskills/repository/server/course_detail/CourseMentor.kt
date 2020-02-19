package com.joshtalks.joshskills.repository.server.course_detail


import com.google.gson.annotations.SerializedName

data class CourseMentor(
    @SerializedName("id")
    var id: Int,
    @SerializedName("from")
    val from: String,
    @SerializedName("image_url")
    val imageUrl: String,
    @SerializedName("name")
    val name: String,
    @SerializedName("qualification_details")
    val qualificationDetails: String
)