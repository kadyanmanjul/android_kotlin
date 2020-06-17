package com.joshtalks.joshskills.repository.service

import com.joshtalks.joshskills.repository.local.model.GaIDMentorModel
import com.joshtalks.joshskills.repository.local.model.RequestRegisterGId
import com.joshtalks.joshskills.repository.local.model.nps.NPSQuestionModel
import com.joshtalks.joshskills.repository.server.*
import com.joshtalks.joshskills.repository.server.feedback.FeedbackStatusResponse
import com.joshtalks.joshskills.repository.server.feedback.RatingDetails
import com.joshtalks.joshskills.repository.server.feedback.UserFeedbackRequest
import kotlinx.coroutines.Deferred
import retrofit2.Response
import retrofit2.http.*
import java.util.*

@JvmSuppressWildcards
interface CommonNetworkService {

    @GET("$DIR/support/category_v2/")
    suspend fun getHelpCategoryV2(): Response<List<TypeOfHelpModel>>

    @POST("$DIR/support/complaint/")
    suspend fun submitComplaint(@Body requestComplaint: RequestComplaint): ComplaintResponse

    @POST("$DIR/mentor/gaid/")
    fun registerGAIdAsync(@Body requestRegisterGId: RequestRegisterGId): Deferred<RequestRegisterGId>

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
}