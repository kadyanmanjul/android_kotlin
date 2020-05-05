package com.joshtalks.joshskills.repository.service

import com.joshtalks.joshskills.repository.local.model.GaIDMentorModel
import com.joshtalks.joshskills.repository.local.model.RequestRegisterGId
import com.joshtalks.joshskills.repository.server.*
import com.joshtalks.joshskills.repository.server.feedback.RatingDetails
import kotlinx.coroutines.Deferred
import retrofit2.Response
import retrofit2.http.*
import java.util.*

@JvmSuppressWildcards
interface CommonNetworkService {

    @GET("$DIR/support/category/")
    suspend fun getHelpCategory(): List<TypeOfHelpModel>


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


    @POST("$DIR/payment/verify/")
    suspend fun verifyPayment(@Body params: Map<String, String>): Any


    @POST("$DIR/course/certificate/generate/")
    suspend fun certificateGenerate(@Body requestCertificateGenerate: RequestCertificateGenerate): Response<CertificateDetail>

    @GET("$DIR/feedback/rating/details/")
    suspend fun getFeedbackRatingDetailsAsync(): List<RatingDetails>


}