package com.joshtalks.joshskills.repository.server


import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class CourseExploreModel(
    @SerializedName("amount")
    val amount: Double,
    @SerializedName("batch_type")
    val batchType: Int,
    @SerializedName("course")
    val course: Int,
    @SerializedName("created")
    val created: String,
    @SerializedName("id")
    val id: Int,
    @SerializedName("is_default")
    val isDefault: Boolean,
    @SerializedName("modified")
    val modified: String,
    @SerializedName("test_name")
    val testName: String,
    @SerializedName("thumbnail")
    val imageUrl: String,
    @SerializedName("course_name")
    val courseName: String
):Serializable