package com.joshtalks.joshskills.repository.service

import com.joshtalks.joshskills.repository.local.model.*
import com.joshtalks.joshskills.repository.server.*
import com.joshtalks.joshskills.repository.server.onboarding.EnrollMentorWithTagIdRequest
import com.joshtalks.joshskills.repository.server.onboarding.EnrollMentorWithTestIdRequest
import com.joshtalks.joshskills.repository.server.onboarding.LogGetStartedEventRequest
import com.joshtalks.joshskills.repository.server.onboarding.OnBoardingStatusResponse
import com.joshtalks.joshskills.repository.server.signup.LoginResponse
import com.joshtalks.joshskills.repository.server.signup.RequestSocialSignUp
import com.joshtalks.joshskills.repository.server.signup.RequestUserVerification
import com.joshtalks.joshskills.repository.server.signup.request.SocialSignUpRequest
import com.joshtalks.joshskills.ui.lesson.speaking.VideoPopupItem
import kotlinx.coroutines.Deferred
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.*

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

    @GET("$DIR/user/sign_out/")
    suspend fun signoutUser(@Query("mentor_id") mentorId: String): Response<Void>

    @GET("$DIR/mentor/{id}/personal_profile/")
    suspend fun getPersonalProfileAsync(@Path("id") id: String): Mentor

    @FormUrlEncoded
    @PATCH("$DIR/user/{id}/")
    suspend fun updateUserProfile(
        @Path("id") userId: String,
        @FieldMap params: Map<String, String?>
    ): Response<User>

    @PATCH("$DIR/user/profile_picture/{id}/")
    suspend fun updateProfilePicFromPreviousProfile(@Path("id") imageId: String): Response<Any>

    @DELETE("$DIR/user/profile_picture/{id}/")
    suspend fun deletePreviousProfilePic(@Path("id") imageId: String): Response<Any>

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
    suspend fun patchFCMToken(
        @Path("id") id: Int,
        @Body params: Map<String, String>
    ): Response<Void>

    @FormUrlEncoded
    @POST("$DIR/mentor/fcm/")
    suspend fun postFCMToken(@FieldMap params: Map<String, String>): Response<FCMResponse>

    @POST("$DIR/mentor/fcm_verify/")
    suspend fun checkFCMInServer(@Body params: Map<String, String>): Map<String, String>

    @POST("$DIR/mentor/install_source")
    suspend fun getInstallReferrerAsync(@Body installReferrerModel: InstallReferrerModel)

    @GET("$DIR/payment/create_order")
    fun getPaymentDetails(@QueryMap params: Map<String, String>): Deferred<Response<PaymentDetailsResponse>>

    @POST("$DIR/payment/create_order_v2")
    fun createPaymentOrder(@Body params: Map<String, String?>): Deferred<Response<OrderDetailResponse>>

    @GET("$DIR/course/test_v2/")
    suspend fun exploreCourses(@QueryMap params: Map<String, String>? = mapOf("is_default" to "true")): List<CourseExploreModel>

    @GET("$DIR/course/subscription_course_list/")
    suspend fun getFreeTrialCourses(): List<CourseExploreModel>

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

//    @POST("$DIR/mentor/last-active")
//    suspend fun activeUser(@Body params: ActiveUserRequest): Response<Any>

    @POST("$DIR/course/buy_expired_course_v2/")
    suspend fun getFreeTrialPaymentData(@Body params: Map<String, Any>): FreeTrialPaymentResponse

    @GET("$DIR/user/profile_pictures//")
    suspend fun getPreviousProfilePics(): Response<PreviousProfilePictures>

    @GET("$DIR/course/course_syllabus/")
    suspend fun getD2pSyllabusPdf() : Response<Map<String, String?>>

    @GET("$DIR/course/language/")
    suspend fun getAvailableLanguageCourses() : Response<List<ChooseLanguages>>
}
