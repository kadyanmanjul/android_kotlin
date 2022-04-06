package com.joshtalks.joshskills.repository.server

import com.google.gson.annotations.SerializedName

data class GaIdResponse(
    @SerializedName("gaid_id")
    val gaID: String
)
