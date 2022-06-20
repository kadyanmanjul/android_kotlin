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
)