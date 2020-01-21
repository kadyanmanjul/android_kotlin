package com.joshtalks.joshskills.repository.server


import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class TypeOfHelpModel(
    @SerializedName("category_name")
    val categoryName: String,
    @SerializedName("icon_url")
    val iconUrl: String,
    @SerializedName("id")
    val id: Int,
    @SerializedName("mobile")
    val mobile: String,
    @SerializedName("moc")
    val moc: List<String>,
    @SerializedName("type")
    val type: String
) : Serializable