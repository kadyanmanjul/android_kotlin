package com.joshtalks.joshskills.common.repository.server.onboarding

import com.google.gson.annotations.SerializedName

class LogGetStartedEventRequest(
    @SerializedName("onboarding_version_id")
    var onboardingVersionId: Int,
    @SerializedName("mentor_id")
    val mentorId: String)
