package com.joshtalks.joshskills.repository.server


import com.google.gson.annotations.SerializedName

data class PreviousLeaderboardResponse(

    @SerializedName("above_three_mentor_list")
    val aboveThreeMentorList: List<LeaderboardMentor>?,
    @SerializedName("award_url")
    val awardUrl: String?,
    @SerializedName("below_three_mentor_list")
    val belowThreeMentorList: List<LeaderboardMentor>?,
    @SerializedName("current_mentor")
    val currentMentor: LeaderboardMentor?,
    @SerializedName("last_mentor_list")
    val lastMentorList: List<LeaderboardMentor>?,
    @SerializedName("title")
    val title: String?,
    @SerializedName("top_50_mentor_list")
    val top50MentorList: List<LeaderboardMentor>?
)
