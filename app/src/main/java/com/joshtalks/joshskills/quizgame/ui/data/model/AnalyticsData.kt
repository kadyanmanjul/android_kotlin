package com.joshtalks.joshskills.quizgame.ui.data.model

import com.google.gson.annotations.SerializedName
import com.joshtalks.joshskills.quizgame.ui.data.network.GAME_ANALYTICS_EVENTS_API_KEY
import com.joshtalks.joshskills.quizgame.ui.data.network.GAME_ANALYTICS_MENTOR_ID_API_KEY

data class AnalyticsData(
    @SerializedName(GAME_ANALYTICS_MENTOR_ID_API_KEY) var userId: String,
    @SerializedName(GAME_ANALYTICS_EVENTS_API_KEY) var event: String
)