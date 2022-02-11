package com.joshtalks.joshskills.ui.voip.voip_rating.model

import androidx.lifecycle.MutableLiveData
import com.google.gson.annotations.SerializedName

data class ReportModel(
    @SerializedName("message")
    val message: String,
    @SerializedName("options")
    val options: List<OptionModel>
)