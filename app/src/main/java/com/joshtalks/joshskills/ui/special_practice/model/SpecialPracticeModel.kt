package com.joshtalks.joshskills.ui.special_practice.model


import com.google.gson.annotations.SerializedName
import com.joshtalks.joshskills.base.local.dao.special_practice.SpecialPractice

data class SpecialPracticeModel(
    @SerializedName("recorded_video_url")
    val recordedVideoUrl: String?,
    @SerializedName("special_practice")
    val specialPractice: SpecialPractice?
)