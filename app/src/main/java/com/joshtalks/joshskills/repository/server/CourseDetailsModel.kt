package com.joshtalks.joshskills.repository.server


import com.google.gson.annotations.SerializedName

data class CourseDetailsModel(
    @SerializedName("id")
    val id: Int,
    @SerializedName("image_url")
    val imageUrl: String,
    @SerializedName("sequence_no")
    val sequenceNo: Int,
    @SerializedName("test")
    val test: Int
)