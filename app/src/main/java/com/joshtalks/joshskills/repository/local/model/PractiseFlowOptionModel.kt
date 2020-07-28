package com.joshtalks.joshskills.repository.local.model

import com.google.gson.annotations.SerializedName

data class PractiseFlowOptionModel(
    @SerializedName("header") val header: String,
    @SerializedName("sub_header") val subHeader: String
)
