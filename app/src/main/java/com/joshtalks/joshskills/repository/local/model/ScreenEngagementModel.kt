package com.joshtalks.joshskills.repository.local.model

import java.io.Serializable

data class ScreenEngagementModel(
    var screenName:String="",
    var startTime: Long = 0,
    var endTime: Long = 0,
    var totalSpendTime: Long = 0
) : Serializable
