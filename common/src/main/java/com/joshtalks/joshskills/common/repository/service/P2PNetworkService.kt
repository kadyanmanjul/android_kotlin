package com.joshtalks.joshskills.common.repository.service

import com.joshtalks.joshskills.voip.base.constants.DIR
import com.joshtalks.joshskills.common.repository.local.entity.practise.FavoriteCaller
import com.joshtalks.joshskills.common.repository.local.model.FirestoreNotificationObject
import com.joshtalks.joshskills.common.repository.local.model.KFactor
import java.util.HashMap
import com.joshtalks.joshskills.common.ui.fpp.PendingRequestResponse
import com.joshtalks.joshskills.common.ui.fpp.RecentCallResponse
import com.joshtalks.joshskills.common.ui.voip.new_arch.ui.models.InterestModel
import com.joshtalks.joshskills.common.ui.voip.new_arch.ui.models.VoipStatusResponse

import retrofit2.Response
import retrofit2.http.*

interface P2PNetworkService {

    @POST("${DIR}/voicecall/agora_token/")
    suspend fun sendAgoraTokenConformation(@Body params: Map<String, String?>): Response<HashMap<String, String>>

    @POST("${DIR}/voicecall/agora_call_response/")
    suspend fun getAgoraCallResponse(@Body params: Map<String, String?>): Response<HashMap<String, String>>

    @POST("${DIR}/voicecall/agora_start_recording/")
    suspend fun startP2PCallRecording(@Body params: Map<String, String?>): HashMap<String, String>

    @POST("${DIR}/voicecall/agora_stop_recording/")
    suspend fun stopP2PCallRecording(@Body params: Map<String, String?>): Any

    @GET("${DIR}/voicecall/agora_user_profile/{id}/")
    suspend fun getUserDetailOnCall(@Path("id") id: String): HashMap<String, String>

    @GET("${DIR}/voicecall/agora_user_profile_v2/{id}/")
    suspend fun getTopicImage(@Path("id") id: String): HashMap<String, String>

    @POST("${DIR}/voicecall/agora_call_feedback/")
    suspend fun p2pCallFeedbackV2(@Body params: Map<String, String?>): Response<KFactor>//FeedbackVoipResponse

    @GET("${DIR}/voicecall/favourites/{mentorId}/")
    suspend fun getFavoriteCallerList(@Path("mentorId") mentorId: String): List<FavoriteCaller>

    @POST("${DIR}/voicecall/favourites/{mentorId}/")
    suspend fun removeFavoriteCallerList(
        @Path("mentorId") mentorId: String,
        @Body requestObj: HashMap<String, List<Int>>
    ): Response<Void>

    @POST("${DIR}/voicecall/agora_favourite_token/")
    suspend fun getFavoriteUserAgoraToken(@Body params: Map<String, String>): Response<HashMap<String, String>>

    @POST("${DIR}/voicecall/agora_new_student_token/")
    suspend fun getNewUserAgoraToken(@Body params: Map<String, String>): Response<HashMap<String, String>>

    @GET("${DIR}/voicecall/agora_fake_call/")
    suspend fun getFakeCall(): FirestoreNotificationObject

    @JvmSuppressWildcards
    @POST("${DIR}/voicecall/agora_call_feedback_submit/")
    suspend fun sendP2pCallReportSubmit(@Body params: Map<String, Any>):Response<Unit>

    @GET("${DIR}/fpp/get_recent_calls/")
    suspend fun getRecentCallsList(@Query("mentor_id") mentorId: String) : Response<RecentCallResponse>

    @POST("${DIR}/fpp/requests/{reciever_mentor_id}/")
    suspend fun sendFppRequest(@Path("reciever_mentor_id") mentorId: String,@Body params: Map<String, String>):Response<Any>

    @DELETE("${DIR}/fpp/requests/{reciever_mentor_id}/")
    suspend fun deleteFppRequest(@Path("reciever_mentor_id") mentorId: String):Response<Any>

    @PATCH("${DIR}/fpp/requests/{sender_mentor_id}/")
    suspend fun confirmOrRejectFppRequest(
        @Path("sender_mentor_id") mentorId: String,
        @Body params: Map<String, String>
    ):Response<Any>
    @GET("${DIR}/fpp/pending_requests/")
    suspend fun getPendingRequestsList() : Response<PendingRequestResponse>

    @POST("${DIR}/fpp/check_already_on_call/")
    suspend fun checkUserInCallOrNot(@Body params : Map<String,String>) : Response<HashMap<String,String>>

    @POST("${DIR}/fpp/block/")
    suspend fun blockFppUser(@Body params : Map<String,String>) : Response<Any>

    @POST("${DIR}/fpp/fpp_dialog/")
    suspend fun showFppDialog(@Body params: HashMap<String, String?>) : Response<HashMap<String,String>>

    @JvmSuppressWildcards
    @POST("${DIR}/voicecall/call_rating/")
    suspend fun submitCallRatings(@Body params: HashMap<String, Any?>) : Response<Any>

    @POST("${DIR}/voicecall/agora_new_topic/")
    suspend fun saveTopicUrlImpression(@Body params : HashMap<String,Any?>) :Response<Any>

    @GET("${DIR}/p2p/status/")
    suspend fun getVoipNewArchFlag(): VoipStatusResponse

    @POST("${DIR}/fpp/fpp_option/")
    suspend fun showFppDialogNew(@Body params: HashMap<String, Int?>) : Response<HashMap<String,Int>>

    @POST("${DIR}/p2p/speaking-level/")
    suspend fun sendUserSpeakingLevel(@Body params: HashMap<String, Int>) : Response<Any>

    @GET("${DIR}/p2p/interests/")
    suspend fun getUserInterestDetails() : Response<InterestModel>

    @POST("${DIR}/p2p/interests/")
    suspend fun sendUserInterestDetails(@Body params: HashMap<String,List<Int>>) : Response<Any>

    @GET("${DIR}/p2p/show-level-interests-screen/")
    suspend fun getFormSubmitStatus() : Response<HashMap<String,Int>>

}
