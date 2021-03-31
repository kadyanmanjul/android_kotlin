package com.joshtalks.joshskills.repository.server.points


import com.google.gson.annotations.SerializedName

data class SpokenMinutesHistoryResponse(
    @SerializedName("spoken_history_date_list")
    val spokenHistoryDateList: List<SpokenHistoryDate>?,
    @SerializedName("total_minutes_spoken")
    val totalMinutesSpoken: Double?,
    @SerializedName("total_minutes_spoken_text")
    val totalMinutesSpokenText: String?
)

data class SpokenHistoryDate(
    @SerializedName("date")
    val date: String?,
    @SerializedName("spoken_history_list")
    val spokenHistoryList: List<SpokenHistory>?,
    @SerializedName("spoken_sum")
    val SpokenSum: Double?
)

data class SpokenHistory(
    @SerializedName("spoken_duration")
    val spokenDuration: Double?,
    @SerializedName("sub_title")
    val subTitle: String?,
    @SerializedName("title")
    val title: String?
)
