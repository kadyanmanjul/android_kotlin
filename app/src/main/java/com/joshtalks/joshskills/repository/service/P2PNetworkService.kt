package com.joshtalks.joshskills.repository.service

import com.joshtalks.joshskills.base.constants.DIR
import com.joshtalks.joshskills.repository.local.entity.practise.FavoriteCaller
import com.joshtalks.joshskills.repository.local.model.FirestoreNotificationObject
import com.joshtalks.joshskills.repository.local.model.KFactor
import com.joshtalks.joshskills.repository.server.voip.AgoraTokenRequest
import com.joshtalks.joshskills.repository.server.voip.RequestUserLocation
import com.joshtalks.joshskills.ui.voip.new_arch.ui.report.model.ReportModel
import java.util.HashMap
import retrofit2.Response
import retrofit2.http.*

interface P2PNetworkService {

    @POST("$DIR/voicecall/agora_token/")
    suspend fun getAgoraClientToken(@Body params: AgoraTokenRequest): Response<HashMap<String, String>>

    @POST("$DIR/voicecall/agora_token/")
    suspend fun sendAgoraTokenConformation(@Body params: Map<String, String?>): Response<HashMap<String, String>>

    @POST("$DIR/voicecall/agora_call_response/")
    suspend fun getAgoraCallResponse(@Body params: Map<String, String?>): Response<HashMap<String, String>>

    @POST("$DIR/voicecall/agora_start_recording/")
    suspend fun startP2PCallRecording(@Body params: Map<String, String?>): HashMap<String, String>

    @POST("$DIR/voicecall/agora_stop_recording/")
    suspend fun stopP2PCallRecording(@Body params: Map<String, String?>): Any

    @GET("$DIR/voicecall/agora_user_profile/{id}/")
    suspend fun getUserDetailOnCall(@Path("id") id: String): HashMap<String, String>

    @POST("$DIR/voicecall/agora_call_location/")
    suspend fun uploadUserLocationAgora(@Body params: RequestUserLocation): Response<Void>

    @POST("$DIR/voicecall/agora_call_feedback/")
    suspend fun p2pCallFeedbackV2(@Body params: Map<String, String?>): Response<KFactor>//FeedbackVoipResponse

    @GET("$DIR/voicecall/favourites/{mentorId}/")
    suspend fun getFavoriteCallerList(@Path("mentorId") mentorId: String): List<FavoriteCaller>

    @POST("$DIR/voicecall/favourites/{mentorId}/")
    suspend fun removeFavoriteCallerList(
        @Path("mentorId") mentorId: String,
        @Body requestObj: HashMap<String, List<Int>>
    ): Response<Void>

    @POST("$DIR/voicecall/agora_favourite_token/")
    suspend fun getFavoriteUserAgoraToken(@Body params: Map<String, String>): Response<HashMap<String, String>>

    @POST("$DIR/voicecall/agora_new_student_token/")
    suspend fun getNewUserAgoraToken(@Body params: Map<String, String>): Response<HashMap<String, String>>

    @GET("$DIR/voicecall/agora_fake_call/")
    suspend fun getFakeCall(): FirestoreNotificationObject

    @JvmSuppressWildcards
    @POST("$DIR/voicecall/agora_call_feedback_submit/")
    suspend fun sendP2pCallReportSubmit(@Body params: Map<String, Any>):Response<Unit>

    @GET("$DIR/voicecall/agora_call_feedback_options/{value}")
    suspend fun getP2pCallOptions(@Path("value") value: String): ReportModel
}
