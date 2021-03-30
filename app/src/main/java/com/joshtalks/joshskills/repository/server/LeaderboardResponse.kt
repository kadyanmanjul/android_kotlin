package com.joshtalks.joshskills.repository.server


import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize

@Parcelize
data class LeaderboardResponse(
    /*
    info: "Student of the Week is announced at 10 AM on every Monday"
    interval_tab_text: "(1 day left)"
    interval_type: "WEEK"
    */

    @SerializedName("info")
    val info: String?,
    @SerializedName("interval_tab_text")
    val intervalTabText: String?,
    @SerializedName("interval_type")
    val intervalType: String?,
    @SerializedName("last_winner")
    val lastWinner: LeaderboardMentor?,
    @SerializedName("current_mentor")
    val current_mentor: LeaderboardMentor?,
    @SerializedName("top_50_mentor_list")
    val top_50_mentor_list: List<LeaderboardMentor>?,
    @SerializedName("above_three_mentor_list")
    val above_three_mentor_list: List<LeaderboardMentor>?,
    @SerializedName("below_three_mentor_list")
    val below_three_mentor_list: List<LeaderboardMentor>?,
    @SerializedName("last_mentor_list")
    val last_mentor_list: List<LeaderboardMentor>?,
    @SerializedName("above_list_total_pages")
    val totalpage: Int = 1,
) : Parcelable

enum class LeaderboardType {
    TODAY,
    WEEK,
    MONTH,
    LIFETIME
}

