package com.joshtalks.joshskills.premium.ui.special_practice.model

import com.google.gson.annotations.SerializedName

data class SaveVideoModel(
    @SerializedName("mentor_id")
    var mentorId: String,
    @SerializedName("video_url")
    var videoUrl: String,
    @SerializedName("special_practice_id")
    var specialPracticeId: String
)