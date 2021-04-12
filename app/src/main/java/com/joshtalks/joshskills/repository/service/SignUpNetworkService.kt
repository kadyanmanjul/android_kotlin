package com.joshtalks.joshskills.repository.service

import com.joshtalks.joshskills.repository.local.model.DeviceDetailsResponse
import com.joshtalks.joshskills.repository.local.model.FCMResponse
import com.joshtalks.joshskills.repository.local.model.InstallReferrerModel
import com.joshtalks.joshskills.repository.local.model.LastLoginResponse
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.repository.local.model.User
import com.joshtalks.joshskills.repository.server.ActiveUserRequest
import com.joshtalks.joshskills.repository.server.CouponCodeResponse
import com.joshtalks.joshskills.repository.server.CourseExploreModel
import com.joshtalks.joshskills.repository.server.CreateOrderResponse
import com.joshtalks.joshskills.repository.server.InstanceIdResponse
import com.joshtalks.joshskills.repository.server.OrderDetailResponse
import com.joshtalks.joshskills.repository.server.PaymentDetailsResponse
import com.joshtalks.joshskills.repository.server.PaymentSummaryResponse
import com.joshtalks.joshskills.repository.server.ProfileResponse
import com.joshtalks.joshskills.repository.server.ReferralCouponDetailResponse
import com.joshtalks.joshskills.repository.server.RequestVerifyOTP
import com.joshtalks.joshskills.repository.server.TrueCallerLoginRequest
import com.joshtalks.joshskills.repository.server.UpdateDeviceRequest
import com.joshtalks.joshskills.repository.server.UpdateUserLocality
import com.joshtalks.joshskills.repository.server.onboarding.EnrollMentorWithTagIdRequest
import com.joshtalks.joshskills.repository.server.onboarding.EnrollMentorWithTestIdRequest
import com.joshtalks.joshskills.repository.server.onboarding.LogGetStartedEventRequest
import com.joshtalks.joshskills.repository.server.onboarding.OnBoardingStatusResponse
import com.joshtalks.joshskills.repository.server.signup.LoginResponse
import com.joshtalks.joshskills.repository.server.signup.RequestSocialSignUp
import com.joshtalks.joshskills.repository.server.signup.RequestUserVerification
import com.joshtalks.joshskills.repository.server.signup.request.SocialSignUpRequest
import kotlinx.coroutines.Deferred
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.FieldMap
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.QueryMap

const val DIR = "api/skill/v1"

@JvmSuppressWildcards
interface SignUpNetworkService {

    @POST("$DIR/user/{path}/")
    suspend fun socialLogin(
        @Path("path") path: String,
        @Body requestSocialSignUp: RequestSocialSignUp
    ): LoginResponse

    @POST("$DIR/mentor/instance/")
    suspend fun getInstanceIdAsync(@Body params: Map<String, String?>): InstanceIdResponse

    @GET("$DIR/user/login/")
    suspend fun getOtpForNumberAsync(@QueryMap params: Map<String, String>)

    @POST("$DIR/user/otp_verify/")
    suspend fun verifyOTP(@Body requestVerifyOTP: RequestVerifyOTP): Response<LoginResponse>

    @POST("$DIR/user/truecaller/login/")
    suspend fun verifyViaTrueCaller(@Body requestVerifyOTP: TrueCallerLoginRequest): Response<LoginResponse>

    @POST("$DIR/user/verify_user/")
    suspend fun verifyGuestUser(@Body socialSignUpRequest: SocialSignUpRequest): Response<LoginResponse>

    @GET("$DIR/version/get_onboarding_status/")
    suspend fun getOnBoardingStatus(
        @Query("instance_id") instanceId: String,
        @Query("mentor_id") mentorId: String,
        @Query("gaid") gaId: String
    ): Response<OnBoardingStatusResponse>

    @POST("$DIR/user/user_verification/")
    suspend fun userVerification(@Body requestUserVerification: RequestUserVerification): Response<LoginResponse>

    @GET("$DIR/mentor/{id}/personal_profile/")
    suspend fun getPersonalProfileAsync(@Path("id") id: String): Mentor

    @FormUrlEncoded
    @PATCH("$DIR/user/{id}/")
    suspend fun updateUserProfile(
        @Path("id") userId: String,
        @FieldMap params: Map<String, String?>
    ): Response<User>

    @PATCH("$DIR/mentor/{id}/")
    suspend fun updateUserAddressAsync(
        @Path("id") id: String,
        @Body params: UpdateUserLocality
    ): ProfileResponse

    @PATCH("$DIR/mentor/{id}/last_login/")
    suspend fun userActive(
        @Path("id") id: String,
        @Body params: Map<String, Any?>
    ): Response<LastLoginResponse>

    @Multipart
    @POST("$DIR/user/{id}/upload_profile_pic/")
    suspend fun uploadProfilePicture(@Path("id") id: String, @Part file: MultipartBody.Part): Any

    @POST("$DIR/mentor/devices_v2/")
    suspend fun postDeviceDetails(@Body obj: UpdateDeviceRequest): DeviceDetailsResponse

    @PATCH("$DIR/mentor/devices_v2/{id}/")
    suspend fun patchDeviceDetails(
        @Path("id") id: Int,
        @Body obj: UpdateDeviceRequest
    ): DeviceDetailsResponse

    @PATCH("$DIR/mentor/fcm/{id}/")
    suspend fun patchFCMToken(@Path("id") id: Int, @Body params: Map<String, String>): Response<Void>

    @FormUrlEncoded
    @POST("$DIR/mentor/fcm/")
    suspend fun postFCMToken(@FieldMap params: Map<String, String>): FCMResponse

    @POST("$DIR/mentor/install_source")
    suspend fun getInstallReferrerAsync(@Body installReferrerModel: InstallReferrerModel)

    @GET("$DIR/payment/create_order")
    fun getPaymentDetails(@QueryMap params: Map<String, String>): Deferred<Response<PaymentDetailsResponse>>

    @POST("$DIR/payment/create_order_v2")
    fun createPaymentOrder(@Body params: Map<String, String?>): Deferred<Response<OrderDetailResponse>>

    @GET("$DIR/course/test/")
    suspend fun exploreCourses(@QueryMap params: Map<String, String>? = mapOf("is_default" to "true")): List<CourseExploreModel>

    @GET("$DIR/payment/coupon/")
    fun validateOrGetAndReferralOrCouponAsync(@QueryMap params: Map<String, String>): Deferred<List<CouponCodeResponse>>

    @GET("$DIR/payment/summary/")
    suspend fun getPaymentSummaryDetails(@QueryMap params: Map<String, String>): PaymentSummaryResponse

    @GET("$DIR/payment/validate/coupon/")
    suspend fun validateAndGetReferralDetails(@QueryMap params: Map<String, String>): ReferralCouponDetailResponse

    @POST("$DIR/payment/create_free_order/")
    suspend fun createFreeOrder(@Body params: CreateOrderResponse): Response<Any>

    @GET("$DIR/course/explore_type_test/")
    suspend fun getSubscriptionTestDetails(
        @Query("gaid") gaid: String
    ): Response<CourseExploreModel>

    @POST("$DIR/user/create_user/")
    suspend fun createGuestUser(@Body params: Map<String, String>): LoginResponse

    @POST("$DIR/mentor/enroll_mentor_test/")
    suspend fun enrollMentorWithTestIds(@Body params: EnrollMentorWithTestIdRequest): Response<Any>

    @POST("$DIR/mentor/enroll_mentor_tags/")
    suspend fun enrollMentorWithTagIds(@Body params: EnrollMentorWithTagIdRequest): Response<Any>

    @POST("$DIR/version/get_started/")
    suspend fun logGetStartedEvent(@Body params: LogGetStartedEventRequest): Response<Any>

    @POST("$DIR/engage/inbox/")
    suspend fun logInboxEngageEvent(@Body params: Map<String, String>)

    @POST("$DIR/mentor/last-active")
    suspend fun activeUser(@Body params: ActiveUserRequest): Response<Any>


}
