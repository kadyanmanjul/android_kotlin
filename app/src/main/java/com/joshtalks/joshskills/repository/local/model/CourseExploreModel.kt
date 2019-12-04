package com.joshtalks.joshskills.repository.local.model


import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class CourseExploreModel(
    @SerializedName("active")
    val active: Boolean,
    @SerializedName("image_url")
    val imageUrl: String,
    @SerializedName("name")
    val name: String,
    @SerializedName("order")
    val order: Int,
    @SerializedName("url")
    val url: String
):Serializable