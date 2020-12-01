package com.joshtalks.joshskills.repository.server


import com.google.gson.annotations.SerializedName

data class SubmitExamResponse(
    @SerializedName("award_mentor_list")
    val awardMentorList: List<Award>?,
    @SerializedName("status")
    val status: String?
)