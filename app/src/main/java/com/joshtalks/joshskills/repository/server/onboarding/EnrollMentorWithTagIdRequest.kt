package com.joshtalks.joshskills.repository.server.onboarding

import com.google.gson.annotations.SerializedName

class EnrollMentorWithTagIdRequest(
    @SerializedName("gaid")
    var gaid: String,
    @SerializedName("mentor_id")
    val mentorId: String,
    @SerializedName("tag_ids")
    val test_ids: List<Int>
)
