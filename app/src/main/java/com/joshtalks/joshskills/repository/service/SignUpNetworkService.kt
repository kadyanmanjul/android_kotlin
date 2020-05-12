package com.joshtalks.joshskills.repository.service

import com.joshtalks.joshskills.repository.local.model.FCMResponse
import com.joshtalks.joshskills.repository.local.model.InstallReferrerModel
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.repository.local.model.googlelocation.Locality
import com.joshtalks.joshskills.repository.server.*
import com.joshtalks.joshskills.repository.server.course_detail.CourseDetailsResponse
import kotlinx.coroutines.Deferred
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.*

const val DIR = "api/skill/v1"

@JvmSuppressWildcards
interface SignUpNetworkService {

    @GET("$DIR/user/login/")
    suspend fun getOtpForNumberAsync(@QueryMap params: Map<String, String>): Response<Any>

    @POST("$DIR/user/otp_verify/")
    fun verifyOTP(@Body requestVerifyOTP: RequestVerifyOTP): Deferred<LoginResponse>


    @POST("$DIR/user/truecaller/login/")
    fun verifyViaTrueCaller(@Body requestVerifyOTP: TrueCallerLoginRequest): Deferred<LoginResponse>

    @GET("$DIR/core/meta/")
    fun getCoreMeta(): Deferred<CoreMeta>

    @POST("$DIR/mentor/account_kit/")
    fun accountKitAuthorizationAsync(@Body accountKitRequest: AccountKitRequest): Deferred<CreateAccountResponse>


    @GET("$DIR/mentor/{id}/personal_profile/")
    fun getPersonalProfileAsync(@Path("id") id: String): Deferred<Mentor>


    @PATCH("$DIR/mentor/{id}/")
    fun updateUserAddressAsync(
        @Path("id") id: String,
        @Body params: UpdateUserLocality
    ): Deferred<ProfileResponse>


    @FormUrlEncoded
    @POST("$DIR/mentor/location/locality/")
    fun confirmUserLocationAsync(@FieldMap params: Map<String, String>): Deferred<Locality>

    @PATCH("$DIR/user/{id}/")
    fun updateUserAsync(
        @Path("id") id: String,
        @Body obj: UpdateUserPersonal
    ): Deferred<UpdateProfileResponse>


    @PATCH("$DIR/mentor/{id}/last_login/")
    suspend fun userActive(@Path("id") id: String, @Body obj: Any)

    @Multipart
    @POST("$DIR/user/{id}/upload_profile_pic/")
    suspend fun uploadProfilePicture(@Path("id") id: String, @Part file: MultipartBody.Part): Any

    @POST("$DIR/mentor/devices/")
    suspend fun updateDeviceDetails(@Body obj: UpdateDeviceRequest)

    @PATCH("$DIR/mentor/fcm/{id}/")
    fun updateFCMToken(@Path("id") id: Int, @Body fcmResponse: FCMResponse): Deferred<Any>

    @FormUrlEncoded
    @POST("$DIR/mentor/fcm/")
    fun uploadFCMToken(@FieldMap params: Map<String, String>): Deferred<FCMResponse>

    @FormUrlEncoded
    @POST("$DIR/mentor/register/anonymous/")
    fun registerAnonymousUser(@FieldMap params: Map<String, String>): Deferred<SuccessResponse>


    @POST("$DIR/mentor/install_source")
    suspend fun getInstallReferrerAsync(@Body installReferrerModel: InstallReferrerModel)


    @GET("$DIR/payment/create_order")
    fun getPaymentDetails(@QueryMap params: Map<String, String>): Deferred<Response<PaymentDetailsResponse>>


    @GET("$DIR/course/test/")
    suspend fun explorerCourse(@QueryMap params: Map<String, String> = mapOf("is_default" to "true")): List<CourseExploreModel>

    @GET("$DIR/course/test_images/")
    fun explorerCourseDetails(@QueryMap params: Map<String, String>): Deferred<List<CourseDetailsModel>>


    @GET("$DIR/course/test_details/")
    fun explorerCourseDetailsApiV2Async(@QueryMap params: Map<String, String> = mapOf("is_default" to "true")): Deferred<List<CourseDetailsResponse>>


    @GET("$DIR/payment/coupon/")
    fun validateOrGetAndReferralOrCouponAsync(@QueryMap params: Map<String, String>): Deferred<List<CouponCodeResponse>>


}