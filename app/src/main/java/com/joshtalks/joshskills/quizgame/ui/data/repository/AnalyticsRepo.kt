package com.joshtalks.joshskills.quizgame.ui.data.repository

import com.joshtalks.joshskills.quizgame.ui.data.network.RetrofitInstance

class AnalyticsRepo {
    suspend fun pushAnalyticsToServer(request: Map<String, Any?>) =
        RetrofitInstance.getRetrofitInstance()?.gameImpressionDetails(request)
}