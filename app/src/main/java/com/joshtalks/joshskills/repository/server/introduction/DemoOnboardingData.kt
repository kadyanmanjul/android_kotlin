package com.joshtalks.joshskills.repository.server.introduction

import com.google.gson.annotations.SerializedName

data class DemoOnboardingData(
    @SerializedName("screen_list")
    val screenList: List<Screen>?
)