package com.joshtalks.joshskills.common.repository.server

import com.google.gson.annotations.SerializedName

data class GaIdResponse(
    @SerializedName("gaid_id")
    val gaID: String
)
