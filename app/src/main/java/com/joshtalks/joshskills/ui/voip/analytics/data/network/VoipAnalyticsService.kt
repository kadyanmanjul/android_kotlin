package com.joshtalks.joshskills.ui.voip.analytics.data.network

import com.joshtalks.joshskills.base.constants.DIR
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.PATCH
import retrofit2.http.POST


const val VOIP_ANALYTICS_CALL_ID_API_KEY = "agora_call"
const val VOIP_ANALYTICS_MENTOR_UID_API_KEY = "agora_mentor"
const val VOIP_ANALYTICS_TYPE_API_KEY = "type"
const val VOIP_ANALYTICS_DISCONNECT_API_KEY = "reason_for_failure"
const val VOIP_ANALYTICS_TIMESTAMP_API_KEY = "timestamp"

interface VoipAnalyticsService {
    @PATCH("$DIR/voicecall/agora_call_details/")
    @JvmSuppressWildcards
    suspend fun agoraCallDetails(
        @Body params: Map<String, Any?>
    ): Response<Unit>

    @POST("$DIR/voicecall/agora_mid_call_analytics/")
    @JvmSuppressWildcards
    suspend fun agoraMidCallDetails(
        @Body params: Map<String, Any?>
    ): Response<Unit>
}