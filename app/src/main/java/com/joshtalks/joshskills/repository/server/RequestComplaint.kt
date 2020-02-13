package com.joshtalks.joshskills.repository.server


import com.google.gson.annotations.SerializedName

data class RequestComplaint(
    @SerializedName("category")
    val category: Int,
    @SerializedName("email")
    val email: String,
    @SerializedName("image_url")
    var imageUrl: String?,
    @SerializedName("mobile")
    val mobile: String,
    @SerializedName("name")
    val name: String,
    @SerializedName("problem")
    val problem: String,
    @SerializedName("mobile_info")
    val mobile_info: Any


)