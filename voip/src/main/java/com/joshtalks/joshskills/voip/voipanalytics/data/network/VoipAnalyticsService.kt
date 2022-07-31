package com.joshtalks.joshskills.voip.voipanalytics.data.network

import com.joshtalks.joshskills.base.constants.DIR
import com.joshtalks.joshskills.voip.data.AmazonPolicyResponse
import com.joshtalks.joshskills.voip.data.api.CallRecordingRequest
import kotlinx.coroutines.Deferred
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.FieldMap
import retrofit2.http.FormUrlEncoded
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

    @FormUrlEncoded
    @POST("$DIR/core/signed_url/")
    fun requestUploadMediaAsync(@FieldMap params: Map<String, String>): Deferred<AmazonPolicyResponse>

    @POST("$DIR/voicecall/agora_call_share")
    suspend fun postCallRecordingFile(@Body request : CallRecordingRequest) : Response<Unit>
}