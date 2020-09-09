package com.joshtalks.joshskills.repository.server.onboarding

import com.google.gson.annotations.SerializedName

class EnrollMentorWithTestIdRequest(
    @SerializedName("gaid")
    var gaid: String,
    @SerializedName("mentor_id")
    var mentorId: String?,
    @SerializedName("test_ids")
    val test_ids: List<Int>
)
