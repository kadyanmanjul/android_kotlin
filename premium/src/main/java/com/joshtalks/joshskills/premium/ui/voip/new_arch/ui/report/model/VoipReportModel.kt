package com.joshtalks.joshskills.premium.ui.voip.new_arch.ui.report.model

import com.google.gson.annotations.SerializedName

data class VoipReportModel(
    @SerializedName("message")
    var message: String,
    @SerializedName("options")
    var voipOptions: List<VoipOptionModel>
)