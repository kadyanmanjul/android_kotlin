package com.joshtalks.joshskills.voip.data

import com.google.gson.annotations.SerializedName

data class AmazonPolicyResponse(
    @SerializedName("fields")
    val fields: HashMap<String, String>,
    @SerializedName("url")
    val url: String,
    @SerializedName("points_list") val pointsList: List<String>?
    )