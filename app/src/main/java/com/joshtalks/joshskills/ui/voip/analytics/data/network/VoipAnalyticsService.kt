package com.joshtalks.joshskills.ui.voip.analytics.data.network

import com.joshtalks.joshskills.repository.service.DIR
import java.util.HashMap
import retrofit2.Response
import retrofit2.http.POST

interface VoipAnalyticsService {
    @POST("$DIR/voicecall/agora_token/")
    suspend fun pushVoipAnalyticsToServer(): Response<HashMap<String, String>>
}