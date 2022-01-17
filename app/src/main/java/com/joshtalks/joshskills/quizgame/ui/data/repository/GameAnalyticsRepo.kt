package com.joshtalks.joshskills.quizgame.ui.data.repository

import com.joshtalks.joshskills.quizgame.ui.data.network.GameApiService

class GameAnalyticsRepo(val api : GameApiService?){
    suspend fun pushAnalyticsToServer(request: Map<String, Any?>) =
        api?.gameImpressionDetails(request)
}