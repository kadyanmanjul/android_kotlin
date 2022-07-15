package com.joshtalks.joshskills.repository.service

import com.joshtalks.joshskills.base.constants.DIR
import com.joshtalks.joshskills.base.local.model.*
import com.joshtalks.joshskills.repository.server.*
import com.joshtalks.joshskills.repository.server.onboarding.EnrollMentorWithTagIdRequest
import com.joshtalks.joshskills.repository.server.onboarding.EnrollMentorWithTestIdRequest
import com.joshtalks.joshskills.repository.server.onboarding.LogGetStartedEventRequest
import com.joshtalks.joshskills.repository.server.onboarding.OnBoardingStatusResponse
import com.joshtalks.joshskills.repository.server.signup.LoginResponse
import com.joshtalks.joshskills.repository.server.signup.RequestSocialSignUp
import com.joshtalks.joshskills.repository.server.signup.RequestUserVerification
import com.joshtalks.joshskills.repository.server.signup.request.SocialSignUpRequest
import com.joshtalks.joshskills.ui.userprofile.models.PreviousProfilePictures
import com.joshtalks.joshskills.ui.userprofile.models.UpdateProfilePayload
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

    @POST("$DIR/mentor/device_gaid_id/")
    suspend fun getGaid(@Body params: Map<String, String?>): Response<GaIdResponse>

    @GET("$DIR/user/login/")
    suspend fun getOtpForNumberAsync(@QueryMap params: Map<String, String>)

    @POST("$DIR/user/otp_verify/")
    suspend fun verifyOTP(@Body requestVerifyOTP: RequestVerifyOTP): Response<LoginResponse>

    @POST("$DIR/user/truecaller/login/")
    suspend fun verifyViaTrueCaller(@Body requestVerifyOTP: TrueCallerLoginRequest): Response<LoginResponse>

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

    @PATCH("$DIR/user/user_update/{id}/")
    suspend fun updateUserProfileV2(
        @Path("id") userId: String,
        @Body params: UpdateProfilePayload
    ): Response<Any>

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

    @POST("$DIR/mentor/devices_v2/")
    suspend fun postDeviceDetails(@Body obj: UpdateDeviceRequest): DeviceDetailsResponse

    @PATCH("$DIR/mentor/devices_v2/{id}/")
    suspend fun patchDeviceDetails(
        @Path("id") id: Int,
        @Body obj: UpdateDeviceRequest
    ): DeviceDetailsResponse

    @FormUrlEncoded
    @POST("$DIR/mentor/fcm/")
    suspend fun postFCMToken(@FieldMap params: Map<String, String>): Response<FCMResponse>

    @POST("$DIR/mentor/fcm_verify/")
    suspend fun checkFCMInServer(@Body params: Map<String, String>): Map<String, String>

    @POST("$DIR/mentor/install_source")
    suspend fun getInstallReferrerAsync(@Body installReferrerModel: InstallReferrerModel)

    @POST("$DIR/payment/create_order_v2")
    fun createPaymentOrder(@Body params: Map<String, String?>): Deferred<Response<OrderDetailResponse>>

    @GET("$DIR/course/test_v2/")
    suspend fun exploreCourses(@QueryMap params: Map<String, String>? = mapOf("is_default" to "true")): List<CourseExploreModel>

    @GET("$DIR/course/subscription_course_list/")
    suspend fun getFreeTrialCourses(): List<CourseExploreModel>

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

    @POST("$DIR/course/buy_expired_course_v2/")
    suspend fun getFreeTrialPaymentData(@Body params: Map<String, Any>): Response<FreeTrialPaymentResponse>

    @GET("$DIR/course/language/")
    suspend fun getAvailableLanguageCourses() : Response<List<ChooseLanguages>>

    @POST("$DIR/mentor/register/")
    suspend fun registerCourse(@Body requestData: HashMap<String, String>): Response<Unit>
}
