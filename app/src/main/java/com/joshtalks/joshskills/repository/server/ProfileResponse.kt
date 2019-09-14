package com.joshtalks.joshskills.repository.server

import com.google.gson.annotations.SerializedName
import com.joshtalks.joshskills.repository.local.model.googlelocation.Locality

data class ProfileResponse(
    @SerializedName("about")
    val about: String,
    @SerializedName("created_by")
    val created_by: Any,
    @SerializedName("id")
    val id: String,
    @SerializedName("locality")
    val locality: Locality,
    @SerializedName("mentor_name")
    val mentor_name: String,
    @SerializedName("user_id")
    val user_id: String
)
