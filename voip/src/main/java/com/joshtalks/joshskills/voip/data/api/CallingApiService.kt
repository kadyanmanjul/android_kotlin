package com.joshtalks.joshskills.voip.data.api

import com.joshtalks.joshskills.base.constants.DIR
import com.joshtalks.joshskills.voip.data.AmazonPolicyResponse
import kotlinx.coroutines.Deferred
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.FieldMap
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

@JvmSuppressWildcards
interface CallingApiService {
    @POST("$DIR/p2p/call")
    suspend fun setUpConnection(@Body request : ConnectionRequest) : HashMap<String, Any?>

    @POST("$DIR/p2p/call_response")
    suspend fun callAccept(@Body request : CallActionRequest) : Response<Unit>

    @POST("$DIR/p2p/call_response")
    suspend fun disconnectCall(@Body request : CallDisconnectRequest) : Response<Unit>

    @FormUrlEncoded
    @POST("$DIR/core/signed_url/")
    fun requestUploadMediaAsync(@FieldMap params: Map<String, String>): Deferred<AmazonPolicyResponse>

    @POST("$DIR/voicecall/agora_call_share")
    suspend fun postCallRecordingFile(@Body request : CallRecordingRequest) : Response<Unit>
}


//"mentor_id": "dc83c689-c914-4694-b132-e93faf0aab23",
//"topic_id": 14,
//"course_id": 151

//call_id: call_id , response:ACCEPT/DECLINE, mentor_id:mentor_id (edited)