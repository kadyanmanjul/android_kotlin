package com.joshtalks.joshskills.ui.lesson.speaking

import com.google.gson.annotations.SerializedName

data class VideoPopupItem(

    @field:SerializedName("name")
    val name: String,

    @field:SerializedName("videolink")
    val videoLink: String
)