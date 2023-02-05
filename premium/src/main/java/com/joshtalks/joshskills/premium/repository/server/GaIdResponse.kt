package com.joshtalks.joshskills.premium.repository.server

import com.google.gson.annotations.SerializedName

data class GaIdResponse(
    @SerializedName("gaid_id")
    val gaID: String
)
