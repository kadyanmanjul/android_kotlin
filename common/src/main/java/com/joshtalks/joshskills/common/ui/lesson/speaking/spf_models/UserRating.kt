package com.joshtalks.joshskills.common.ui.lesson.speaking.spf_models

import com.google.gson.annotations.SerializedName

data class UserRating(
    @field:SerializedName("rating")
    val rating: Double,

    @field:SerializedName("bg_color")
    val bgColor: String,

    @field:SerializedName("color")
    val color: String,
)
