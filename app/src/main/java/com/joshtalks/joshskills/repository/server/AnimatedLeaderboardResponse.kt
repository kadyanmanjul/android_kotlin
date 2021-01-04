package com.joshtalks.joshskills.repository.server


import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize

@Parcelize
data class AnimatedLeaderBoardResponse(

    @SerializedName("award_url")
    val awardUrl: String?,
    @SerializedName("title")
    val title: String?,
    @SerializedName("current_mentor")
    val currentMentor: LeaderboardMentor?,
    @SerializedName("leader_board_mentor_list")
    val leaderBoardMentorList: List<LeaderboardMentor>?,
    @SerializedName("above_mentor_list")
    val aboveMentorList: List<LeaderboardMentor>?
) : Parcelable
