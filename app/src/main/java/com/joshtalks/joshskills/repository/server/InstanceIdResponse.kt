package com.joshtalks.joshskills.repository.server

import com.google.gson.annotations.SerializedName

data class InstanceIdResponse(
    @SerializedName("id")
    val instanceId: String
)
