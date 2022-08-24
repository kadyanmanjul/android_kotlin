package com.joshtalks.joshskills.repository.server.voip

import com.google.gson.annotations.SerializedName

class TargetData(
    @SerializedName("target_type")
    val type: String,
    @SerializedName("last_call")
    val lastCall : Int
)

