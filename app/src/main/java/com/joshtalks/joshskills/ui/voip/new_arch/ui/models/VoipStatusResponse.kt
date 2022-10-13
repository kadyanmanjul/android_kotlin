package com.joshtalks.joshskills.ui.voip.new_arch.ui.models

import com.google.gson.annotations.SerializedName

data class VoipStatusResponse(
    @SerializedName("status")
    val status: Int?,
    @SerializedName("speed")
    val thresholdSpeed: Int?,
    @SerializedName("file_url")
    val speedTestFile: String?,
    @SerializedName("file_size")
    val testFileSize: Int?,
    @SerializedName("fpp_group_status")
    val groupFppStatus: Int?,
    @SerializedName("is_game_enabled")
    val isGameOn: Int?,
    @SerializedName("is_beep_timer_enabled")
    val isBeepTimerEnabled: Int?,
    @SerializedName("speaking_enabled")
    val isLevelFormOn: Int?,
    @SerializedName("user_interests_enabled")
    val isInterestFormOn: Int?
)