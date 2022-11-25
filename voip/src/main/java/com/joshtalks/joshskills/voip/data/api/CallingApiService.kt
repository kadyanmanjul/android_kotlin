package com.joshtalks.joshskills.voip.data.api

import com.joshtalks.joshskills.voip.base.constants.DIR
import com.joshtalks.joshskills.voip.base.constants.DIR_FPP_GROUP
import com.joshtalks.joshskills.voip.BuildConfig
import com.joshtalks.joshskills.voip.data.AmazonPolicyResponse
import kotlinx.coroutines.Deferred
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.FieldMap
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

@JvmSuppressWildcards
interface CallingApiService {
    @POST("${DIR}/p2p/call")
    suspend fun startPeerToPeerCall(@Body request : ConnectionRequest) : HashMap<String, Any?>

    @POST("${DIR}/p2p/call_response")
    suspend fun callAccept(@Body request : CallActionRequest) : Response<Unit>

    @POST("${DIR}/p2p/call_response")
    suspend fun disconnectCall(@Body request : CallDisconnectRequest) : Response<Unit>

//    FPP

    @POST("${BuildConfig.MS_BASE_URL}/${DIR_FPP_GROUP}/fpp/call")
    suspend fun startFavouriteCall(@Body request : FavoriteConnectionRequest) : HashMap<String, Any?>

    @POST("${BuildConfig.MS_BASE_URL}/${DIR_FPP_GROUP}/expert/call")
    suspend fun startExpertCall(@Body request: ExpertConnectionRequest) : HashMap<String,Any?>

    @POST("${BuildConfig.MS_BASE_URL}/${DIR_FPP_GROUP}/call_response")
    suspend fun favouriteCallAccept(@Body request : FavoriteCallActionRequest) : Response<Unit>

    @POST("${BuildConfig.MS_BASE_URL}/${DIR_FPP_GROUP}/call_response")
    suspend fun favouriteCallReject(@Body request : FavoriteCallActionRequest) : Response<Unit>

    @POST("${BuildConfig.MS_BASE_URL}/${DIR_FPP_GROUP}/call_response")
    suspend fun favouriteCallDisconnect(@Body request : CallDisconnectRequest) : Response<Unit>

//    GROUP
    @POST("${BuildConfig.MS_BASE_URL}/${DIR_FPP_GROUP}/group/call")
    suspend fun startGroupCall(@Body request : GroupConnectionRequest) : HashMap<String, Any?>

    @POST("${BuildConfig.MS_BASE_URL}/${DIR_FPP_GROUP}/call_response")
    suspend fun groupCallAccept(@Body request : GroupCallActionRequest) : Response<Unit>

    @POST("${BuildConfig.MS_BASE_URL}/${DIR_FPP_GROUP}/call_response")
    suspend fun groupCallReject(@Body request : GroupCallActionRequest) : Response<Unit>

    @POST("${BuildConfig.MS_BASE_URL}/${DIR_FPP_GROUP}/call_response")
    suspend fun groupCallDisconnect(@Body request : CallDisconnectRequest) : Response<Unit>


    @FormUrlEncoded
    @POST("${DIR}/core/signed_url/")
    fun requestUploadMediaAsync(@FieldMap params: Map<String, String>): Deferred<AmazonPolicyResponse>
}


//"mentor_id": "dc83c689-c914-4694-b132-e93faf0aab23",
//"topic_id": 14,
//"course_id": 151

//call_id: call_id , response:ACCEPT/DECLINE, mentor_id:mentor_id (edited)