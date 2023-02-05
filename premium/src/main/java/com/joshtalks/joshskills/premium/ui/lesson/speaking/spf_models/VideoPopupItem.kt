package com.joshtalks.joshskills.premium.ui.lesson.speaking.spf_models

import com.google.gson.annotations.SerializedName

data class VideoPopupItem(

    @field:SerializedName("name")
    val name: String,

    @field:SerializedName("videolink")
    val videoLink: String
)