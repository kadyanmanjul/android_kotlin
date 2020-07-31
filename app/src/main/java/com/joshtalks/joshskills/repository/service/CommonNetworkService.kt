package com.joshtalks.joshskills.repository.service

import com.joshtalks.joshskills.repository.local.model.GaIDMentorModel
import com.joshtalks.joshskills.repository.local.model.RequestRegisterGId
import com.joshtalks.joshskills.repository.local.model.nps.NPSQuestionModel
import com.joshtalks.joshskills.repository.server.CertificateDetail
import com.joshtalks.joshskills.repository.server.ComplaintResponse
import com.joshtalks.joshskills.repository.server.FAQ
import com.joshtalks.joshskills.repository.server.FAQCategory
import com.joshtalks.joshskills.repository.server.FreshChatRestoreIDResponse
import com.joshtalks.joshskills.repository.server.NPSByUserRequest
import com.joshtalks.joshskills.repository.server.RequestCertificateGenerate
import com.joshtalks.joshskills.repository.server.RequestComplaint
import com.joshtalks.joshskills.repository.server.SuccessResponse
import com.joshtalks.joshskills.repository.server.conversation_practice.ConversationPractiseModel
import com.joshtalks.joshskills.repository.server.conversation_practice.SubmitConversationPractiseRequest
import com.joshtalks.joshskills.repository.server.course_detail.CourseDetailsResponseV2
import com.joshtalks.joshskills.repository.server.feedback.FeedbackStatusResponse
import com.joshtalks.joshskills.repository.server.feedback.RatingDetails
import com.joshtalks.joshskills.repository.server.feedback.UserFeedbackRequest
import kotlinx.coroutines.Deferred
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.FieldMap
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.QueryMap
import java.util.*

@JvmSuppressWildcards
interface CommonNetworkService {

    @GET("$DIR/support/category_v2/")
    suspend fun getHelpCategoryV2(): Response<List<FAQCategory>>

    @POST("$DIR/support/complaint/")
    suspend fun submitComplaint(@Body requestComplaint: RequestComplaint): ComplaintResponse

    @POST("$DIR/mentor/gaid/")
    fun registerGAIdAsync(@Body requestRegisterGId: RequestRegisterGId): Deferred<RequestRegisterGId>

    @GET("$DIR/mentor/restore_id/{id}/")
    suspend fun getFreshChatRestoreIdAsync(@Path("id") id: String): FreshChatRestoreIDResponse

    @FormUrlEncoded
    @PATCH("$DIR/mentor/restore_id/{id}/")
    suspend fun postFreshChatRestoreIDAsync(
        @Path("id") id: String,
        @FieldMap params: Map<String, String?>
    ): FreshChatRestoreIDResponse

    @POST("$DIR/mentor/gaid_detail/")
    fun registerGAIdDetailsAsync(@Body params: Map<String, String>): Deferred<GaIDMentorModel>

    @PATCH("$DIR/mentor/gaid_detail/{id}/")
    fun patchMentorWithGAIdAsync(
        @Path("id") id: String,
        @Body params: HashMap<String, @JvmSuppressWildcards List<String>>
    ): Deferred<SuccessResponse>


    @POST("$DIR/payment/verify_v2/")
    suspend fun verifyPayment(@Body params: Map<String, String>): Any

    @POST("$DIR/course/certificate/generate/")
    suspend fun certificateGenerate(@Body requestCertificateGenerate: RequestCertificateGenerate): Response<CertificateDetail>

    @GET("$DIR/feedback/rating/details/")
    suspend fun getFeedbackRatingDetailsAsync(): List<RatingDetails>

    @POST("$DIR/feedback/response/")
    suspend fun postUserFeedback(@Body userFeedbackRequest: UserFeedbackRequest): Response<Any>

    @GET("$DIR/feedback/")
    suspend fun getQuestionFeedbackStatus(@Query("question_id") id: String): Response<FeedbackStatusResponse>

    @GET("$DIR/feedback/nps/details/")
    suspend fun getQuestionNPSEvent(@Query("event_name") eventName: String): Response<List<NPSQuestionModel>>

    @POST("$DIR/feedback/nps/response/")
    suspend fun submitNPSResponse(@Body npsByUserRequest: NPSByUserRequest): Any

    @GET("$DIR/support/faq/")
    suspend fun getFaqList(): List<FAQ>

    @PATCH("$DIR/support/faq/{id}/")
    suspend fun patchFaqFeedback(
        @Path("id") id: String,
        @Body params: Map<String, String?>
    ): FAQ

    @GET("$DIR/course/course_details/")
    suspend fun getCourseDetails(
        @QueryMap params: Map<String, String>
    ): Response<CourseDetailsResponseV2>


    @GET("$DIR/conversation-practice/{id}/")
    suspend fun getConversationPractise(
        @Path("id") id: String
    ): Response<ConversationPractiseModel>

    @POST("$DIR/conversation-practice/submit/")
    suspend fun submitConversationPractice(
        @Body request: SubmitConversationPractiseRequest
    ): Response<Any>


}
