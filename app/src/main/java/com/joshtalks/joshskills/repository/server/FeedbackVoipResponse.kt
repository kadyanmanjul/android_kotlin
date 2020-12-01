package com.joshtalks.joshskills.repository.server


import com.google.gson.annotations.SerializedName

data class FeedbackVoipResponse(
    @SerializedName("award_mentor_list")
    val awardMentorList: List<Award>?,
    @SerializedName("callinfo")
    val callinfo: String?,
    @SerializedName("confidence")
    val confidence: Int?,
    @SerializedName("eagerness")
    val eagerness: Int?,
    @SerializedName("grammar")
    val grammar: Int?,
    @SerializedName("id")
    val id: String?,
    @SerializedName("mentor")
    val mentor: String?,
    @SerializedName("pronunciation")
    val pronunciation: Int?,
    @SerializedName("respectfulness")
    val respectfulness: Int?
)