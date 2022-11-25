package com.joshtalks.joshskills.common.ui.voip.analytics.data.network

import com.joshtalks.joshskills.voip.base.constants.DIR
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.PATCH
import retrofit2.http.POST

interface VoipAnalyticsService {
    @PATCH("${DIR}/voicecall/agora_call_details/")
    @JvmSuppressWildcards
    suspend fun agoraCallDetails(
        @Body params: Map<String, Any?>
    ): Response<Unit>

    @POST("${DIR}/voicecall/agora_mid_call_analytics/")
    @JvmSuppressWildcards
    suspend fun agoraMidCallDetails(
        @Body params: Map<String, Any?>
    ): Response<Unit>
}