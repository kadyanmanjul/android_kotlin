package com.joshtalks.joshskills.ui.voip.new_arch.ui.report.model

import com.google.gson.annotations.SerializedName

data class ReportModel(
    @SerializedName("message")
    var message: String,
    @SerializedName("options")
    var options: List<OptionModel>
)