package com.joshtalks.joshskills.repository.server


import com.google.gson.annotations.SerializedName
import com.joshtalks.joshskills.core.EMPTY
import java.io.Serializable

data class CourseExploreModel(
    @SerializedName("amount")
    var amount: Double,
    @SerializedName("batch_type")
    val batchType: Int,
    @SerializedName("course")
    val course: Int,
    @SerializedName("created")
    val created: String,
    @SerializedName("id")
    var id: Int,
    @SerializedName("is_default")
    val isDefault: Boolean,
    @SerializedName("modified")
    val modified: String,
    @SerializedName("test_name")
    val testName: String,
    @SerializedName("thumbnail")
    var imageUrl: String,
    @SerializedName("course_name")
    var courseName: String,
    @SerializedName("course_duration")
    val courseDuration: String,
    @SerializedName("course_icon")
    var courseIcon: String

) : Serializable {

    constructor() : this(
        amount = 0.0,
        batchType = 0,
        course = 0,
        created = EMPTY,
        id = 0,
        isDefault = false,
        modified = EMPTY,
        testName = EMPTY,
        imageUrl = EMPTY,
        courseName = EMPTY,
        courseDuration = EMPTY,
        courseIcon = EMPTY
    )

}