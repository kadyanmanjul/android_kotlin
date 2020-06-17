package com.joshtalks.joshskills.repository.server

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class TypeOfHelpModel(
    @SerializedName("id")
    val id: Int,
    @SerializedName("category_name")
    val categoryName: String,
    @SerializedName("icon")
    val iconUrl: String
) : Serializable