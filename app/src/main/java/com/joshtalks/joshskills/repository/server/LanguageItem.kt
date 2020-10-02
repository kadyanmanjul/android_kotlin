package com.joshtalks.joshskills.repository.server

import com.google.gson.annotations.SerializedName

data class LanguageItem(
    @SerializedName("code")
    val code: String,
    @SerializedName("name")
    val name: String
)
