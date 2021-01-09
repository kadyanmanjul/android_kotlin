package com.joshtalks.joshskills.repository.server

import com.google.gson.annotations.SerializedName

data class AmazonPolicyResponse(
    @SerializedName("fields")
    val fields: HashMap<String, String>,
    @SerializedName("url")
    val url: String,
    @SerializedName("points_list") val pointsList: List<String>?
    )