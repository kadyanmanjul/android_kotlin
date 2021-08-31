package com.joshtalks.joshskills.repository.server.signup


import com.google.gson.annotations.SerializedName

data class EngagementVersionResponse(
    @SerializedName("engagement_version")
    val engagementVersion: EngagementVersion?
)