package com.joshtalks.joshskills.common.repository.service

import com.joshtalks.joshskills.common.repository.local.entity.BroadCastEvent
import com.joshtalks.joshskills.common.repository.local.entity.engage_notification.AppUsageModel
import com.joshtalks.joshskills.common.repository.local.model.ButtonVisibilityResponse
import com.joshtalks.joshskills.common.repository.local.model.GaIDMentorModel
import com.joshtalks.joshskills.common.repository.local.model.RequestRegisterGAId
import com.joshtalks.joshskills.common.repository.local.model.WalletBalance
import com.joshtalks.joshskills.common.repository.server.*
import com.joshtalks.joshskills.common.repository.server.buypage.CouponListModel
import com.joshtalks.joshskills.common.repository.server.certification_exam.*
import com.joshtalks.joshskills.common.repository.server.conversation_practice.ConversationPractiseModel
import com.joshtalks.joshskills.common.repository.server.conversation_practice.SubmitConversationPractiseRequest
import com.joshtalks.joshskills.common.repository.server.conversation_practice.SubmittedConversationPractiseModel
import com.joshtalks.joshskills.common.repository.server.feedback.RatingDetails
import com.joshtalks.joshskills.common.repository.server.feedback.UserFeedbackRequest
import com.joshtalks.joshskills.common.repository.server.onboarding.CourseEnrolledRequest
import com.joshtalks.joshskills.common.repository.server.onboarding.CourseEnrolledResponse
import com.joshtalks.joshskills.common.repository.server.points.PointsHistoryResponse
import com.joshtalks.joshskills.common.repository.server.points.PointsInfoResponse
import com.joshtalks.joshskills.common.repository.server.points.SpokenMinutesHistoryResponse
import com.joshtalks.joshskills.common.repository.server.reminder.DeleteReminderRequest
import com.joshtalks.joshskills.common.repository.server.reminder.ReminderRequest
import com.joshtalks.joshskills.common.repository.server.translation.WordDetailsResponse
import com.joshtalks.joshskills.common.repository.server.voip.SpeakingTopic
import com.joshtalks.joshskills.common.track.CourseUsageSync
import com.joshtalks.joshskills.common.ui.inbox.adapter.InboxRecommendedCourse
import com.joshtalks.joshskills.common.ui.inbox.payment_verify.VerifyPaymentStatus
import com.joshtalks.joshskills.common.ui.payment.model.VerifyPayment
import com.joshtalks.joshskills.common.ui.senior_student.model.SeniorStudentModel
import com.joshtalks.joshskills.common.ui.special_practice.model.SaveVideoModel
import com.joshtalks.joshskills.common.ui.special_practice.model.SpecialPracticeModel
import com.joshtalks.joshskills.common.ui.userprofile.models.*
import retrofit2.Response
import retrofit2.http.*

@JvmSuppressWildcards
interface CommonNetworkService {

    @GET("$DIR/support/category_v2/")
    suspend fun getHelpCategoryV2(): Response<List<FAQCategory>>

    @GET("$DIR/mentor/restore_id/{id}/")
    suspend fun getFreshChatRestoreIdAsync(@Path("id") id: String): FreshChatRestoreIDResponse

    @FormUrlEncoded
    @PATCH("$DIR/mentor/restore_id/{id}/")
    suspend fun postFreshChatRestoreIDAsync(
        @Path("id") id: String,
        @FieldMap params: Map<String, String?>
    ): FreshChatRestoreIDResponse

    @PATCH("$DIR/mentor/gaid_detail/{id}/")
    suspend fun patchMentorWithGAIdAsync(
        @Path("id") id: Int,
        @Body params: HashMap<String, @JvmSuppressWildcards List<String>>
    ): Response<Any>

    @FormUrlEncoded
    @PATCH("$DIR/mentor/gaid/{id}/")
    suspend fun mergeMentorWithGAId(@Path("id") id: String, @FieldMap params: Map<String, String>)

    @GET("$DIR/payment/verify_v3/")
    suspend fun verifyPaymentV3(@Query("order_id") orderId: String): VerifyPayment

    @POST("$DIR/course/certificate/generate/")
    suspend fun certificateGenerate(@Body requestCertificateGenerate: RequestCertificateGenerate): Response<CertificateDetail>

    @GET("$DIR/feedback/rating/details/")
    suspend fun getFeedbackRatingDetailsAsync(): List<RatingDetails>

    @POST("$DIR/feedback/response/")
    suspend fun postUserFeedback(@Body userFeedbackRequest: UserFeedbackRequest): Response<Any>

    @GET("$DIR/support/faq/")
    suspend fun getFaqList(): List<FAQ>

    @PATCH("$DIR/support/faq/{id}/")
    suspend fun patchFaqFeedback(
        @Path("id") id: String,
        @Body params: Map<String, String?>
    ): FAQ

    @POST("$DIR/course/course_heading/")
    suspend fun getCourseEnrolledDetails(
        @Body params: CourseEnrolledRequest
    ): Response<CourseEnrolledResponse>

    @GET("$DIR/conversation-practice/{id}/")
    suspend fun getConversationPractise(
        @Path("id") id: String
    ): Response<ConversationPractiseModel>

    @POST("$DIR/conversation-practice/submit/")
    suspend fun submitConversationPractice(
        @Body request: SubmitConversationPractiseRequest
    ): Response<SuccessResponse>

    @GET("$DIR/conversation-practice/mentor/")
    suspend fun getSubmittedConversationPractise(@Query("conversationpractice_id") eventName: String): Response<List<SubmittedConversationPractiseModel>>

    @POST("$DIR/mentor/reminders/")
    suspend fun setReminder(@Body requestSetReminderRequest: ReminderRequest): Response<BaseResponse<Int>>

    @POST("$DIR/mentor/delete_reminders/")
    suspend fun deleteReminders(@Body deleteReminderRequest: DeleteReminderRequest): Response<BaseResponse<*>>

    @GET("$DIR/fpp/profile_favourite/{user_profile_mentor_id}/")
    suspend fun getFppStatusInProfile(
        @Path("user_profile_mentor_id") mentorId: String
    ): Response<FppStatusInProfileResponse>

    @GET("$DIR/voicecall/topic/v2/{id}/")
    suspend fun getTopicDetail(@Path("id") id: String): SpeakingTopic

    @GET("$DIR/voicecall/senior_student_info/")
    suspend fun getSeniorStudentData(): SeniorStudentModel

    @GET("$DIR/certificateexam/{id}/")
    suspend fun getCertificateExamDetails(@Path("id") id: Int): CertificationQuestionModel

    @POST("$DIR/certificateexam/v2/report")
    suspend fun submitExam(@Body params: RequestSubmitCertificateExam): Response<Any>

    @GET("$DIR/certificateexam/report")
    suspend fun getExamReports(@Query("certificateexam_id") id: Int): List<CertificateExamReportModel>

    @GET("$DIR/certificateexam/user_details")
    suspend fun getCertificateUserDetails(): Response<CertificationUserDetail?>

    @GET("http://www.postalpincode.in/api/pincode/{pin}")
    suspend fun getInfoFromPinCode(@Path("pin") pin: Int): PostalDetails

    @POST("$DIR/certificateexam/user_details")
    suspend fun submitUserDetailForCertificate(@Body certificationUserDetail: CertificationUserDetail): Map<String, String>?

    @GET("$DIR/group/user_profile_v2/{mentor_id}/")
    suspend fun getUserProfileData(
        @Path("mentor_id") id: String,
        @Query("interval_type") intervalType: String? = null,
        @Query("previous_page") previousPage: String? = null
    ): Response<UserProfileResponse>

    @GET("$DIR/user/user_profile_v2/{mentor_id}/")
    suspend fun getUserProfileDataV3(
        @Path("mentor_id") id: String,
        @Query("interval_type") intervalType: String? = null,
        @Query("previous_page") previousPage: String? = null,
        @Query("?api_version=") apiVersion: Int = 2
    ): Response<UserProfileResponse>

    @GET("$DIR/user/profile_awards/{mentor_id}/")
    suspend fun getProfileAwards(@Path("mentor_id") id: String): Response<AwardHeader>

    @GET("$DIR/user/profile_groups/{mentor_id}/")
    suspend fun getProfileGroups(@Path("mentor_id") id: String): Response<GroupsHeader>

    @GET("$DIR/user/profile_courses/{mentor_id}/")
    suspend fun getProfileCourses(@Path("mentor_id") id: String): Response<CourseHeader>

    @GET("$DIR/user/profile_pictures/{mentor_id}/")
    suspend fun getPreviousProfilePics(@Path("mentor_id") id: String): Response<PictureHeader>

    @GET("$DIR/reputation/get_points_history_v2/")
    suspend fun getUserPointsHistory(
        @Query("mentor_id") id: String,
        @Query("course_id") courseId: String
    ): Response<PointsHistoryResponse>

    @GET("$DIR/reputation/get_spoken_history_v2/")
    suspend fun getUserSpokenMinutesHistory(
        @Query("mentor_id") id: String,
        @Query("course_id") courseId: String
    ): Response<SpokenMinutesHistoryResponse>

    @Headers(
        "Accept: application/json",
        "Content-type:application/json",
        "Cache-Control: public, only-if-cached,  max-stale=640000,  max-age=640000"
    )
    @GET("$DIR/reputation/get_points_working/")
    suspend fun getPointsInfo(): Response<PointsInfoResponse>

    @POST("$DIR/mentor/delete_mentor/")
    suspend fun deleteMentor(@Body params: Map<String, String>): Response<Void>

    @PATCH("$DIR/reputation/award_mentor/")
    suspend fun patchAwardDetails(
        @Body params: HashMap<String, List<Int>>
    ): Response<PointsInfoResponse>

    @Headers(
        "Accept: application/json",
        "Content-type:application/json",
        "Cache-Control: public, only-if-cached,  max-stale=640000,  max-age=640000"
    )
    @GET("$DIR/question/word-detail/")
    suspend fun getWordDetail(@Query("word") word: String): WordDetailsResponse

    @PATCH("$DIR/impression/user_profile_impression/{user_profile_impression_id}/")
    suspend fun engageUserProfileTime(
        @Path("user_profile_impression_id") userProfileImpressionId: String,
        @Body params: Map<String, Long>
    ): WordDetailsResponse

    @POST("$DIR/impression/user_profile_section_impression/")
    suspend fun userProfileSectionImpression(@Body params: Map<String, String>): UserProfileSectionResponse

    @POST("$DIR/impression/certificateexam_impression/")
    suspend fun saveCertificateImpression(@Body params: Map<String, String>): Response<Unit>

    @GET("$DIR/certificateexam/get_exam_type_from_id/")
    suspend fun getCertificateExamType(@Query("exam_id") params: String): Response<Map<String, String>>

    @PATCH("$DIR/impression/user_profile_section_impression/")
    suspend fun engageUserProfileSectionTime(@Body params: Map<String, String>): Any

    @PATCH("$DIR/impression/activity_feed_impression/{activity_feed_impression_id}/")
    suspend fun engageActivityFeedTime(
        @Path("activity_feed_impression_id") userProfileImpressionId: String,
        @Body params: Map<String, Long>
    ): Any

    @POST("$DIR/leaderboard/leaderboard_impression/")
    suspend fun engageLeaderBoardImpressions(
        @Body params: Map<String, String>
    ): Any

    @GET("$DIR/reputation/award_render/")
    suspend fun get3DWebView(
        @Query("award_mentor_id") awardMentorId: String
    ): String

    @POST("$DIR/engage/user-activity/")
    suspend fun engageUserSession(@Body params: HashMap<String, List<AppUsageModel>>): Response<Void>

    @POST("$DIR/engage/course-user-activity/")
    suspend fun engageCourseUsageSession(
        @Body params: HashMap<String, List<CourseUsageSync>>
    ): Response<Void>

    @POST("$DIR/mentor/gaid/")
    suspend fun registerGAIdDetailsV2Async(@Body body: RequestRegisterGAId): GaIDMentorModel

    @POST("$DIR/course/free_trial_register_course/")
    suspend fun enrollFreeTrialMentorWithCourse(@Body params: Map<String, String>): Response<Void>

    @POST("$DIR/impression/track_impressions/")
    suspend fun saveImpression(@Body params: Map<String, String>): Response<Void>

    @POST("$DIR/impression/tcflow_track_impressions/")
    suspend fun saveTrueCallerImpression(@Body params: Map<String, String>): Response<Void>

    @POST("$DIR/impression/track_voicecall_impression/")
    suspend fun saveVoiceCallImpression(@Body params: Map<String, String>): Response<Void>

    @POST("$DIR/payment/verify_payment/")
    suspend fun checkMentorPayStatus(@Body params: Map<String, String>): Map<String, Any>

    @GET("$DIR/payment/verify_razorpay_order/")
    suspend fun syncPaymentStatus(@Query("order_id") orderId: String): Response<VerifyPaymentStatus>

    @POST("$DIR/link_attribution/deep_link/")
    suspend fun getDeepLink(@Body params: LinkAttribution): Response<Any>

    @POST("$DIR/link_attribution/analytics/")
    suspend fun saveDeepLinkImpression(@Body params: Map<String, String>): Response<Any>

    @POST("$DIR/impression/track_course_impressions/")
    suspend fun saveIntroVideoFlowImpression(@Body params: Map<String, Any?>): Response<Any>

    @POST("$DIR/mentor/restart_course/")
    suspend fun restartCourse(@Body params: Map<String, String>): Response<RestartCourseResponse>

    @POST("$DIR/impression/restart_course_track_impressions/")
    suspend fun restartCourseImpression(@Body params: Map<String, String>): Response<Void>

    @POST("$DIR/question/special_practice_details/")
    suspend fun getSpecialPracticeDetails(@Body params: Map<String, String>): Response<SpecialPracticeModel>

    @POST("$DIR/question/special_practice_submit/")
    suspend fun saveVideoOnServer(@Body params: SaveVideoModel): Response<SuccessResponse>

    @POST("$DIR/voicecall/invite/call_invite/")
    suspend fun inviteFriend(@Body params: HashMap<String, String>): Response<Any>

    @POST("$DIR/voicecall/invite/contacts/")
    suspend fun uploadContacts(@Body params: HashMap<String, Any>): Response<Any>

    @POST("$DIR/impression/track_broadcast_event/")
    suspend fun saveBroadcastEvent(@Body params: BroadCastEvent): Response<Unit>

    @POST("$DIR/impression/track_reading_practice_impression/")
    suspend fun saveReadingPracticeImpression(@Body params: Map<String, String>): Response<Void>

    @POST("$DIR/impression/track_audio_impression/")
    suspend fun trackAudioImpression(@Body params: Map<String, String>): Response<Void>

    @GET("$DIR/course/show_popup/")
    suspend fun getCoursePopUpData(
        @Query("course_id") courseId: String,
        @Query("name") popupName: String,
        @Query("call_count") callCount: Int = 0,
        @Query("call_duration") callDuration: Long = 0
    ): Response<PurchaseDataResponse>

    @POST("$DIR/impression/track_micro_payment_impression/")
    suspend fun saveMicroPaymentImpression(@Body params: Map<String, String>)

    @POST("$DIR/impression/track_popup_impression/")
    suspend fun savePopupImpression(@Body params: Map<String, String>): Response<Void>

    @POST("$DIR/impression/track_buy_course_impression/")
    suspend fun saveNewBuyPageLayoutImpression(@Body params: Map<String, String>): Response<Void>

    @GET("$DIR/micro_payment/expert_call_status/")
    suspend fun getButtonExpertVisibility(): Response<ButtonVisibilityResponse>

    @POST("$DIR/support/sales_support/")
    suspend fun saveSalesSupportReason(@Body params: Map<String, String>): Response<Any>

    @POST("$DIR/impression/track_explore_course_impression/")
    suspend fun saveImpressionForExplore(@Body params: Map<String, String>): Response<Void>

    @GET("$DIR/course/get_popup_names/")
    suspend fun getPopupType(): Response<HashMap<String, Boolean>>

    @GET("$DIR/course/course_recommendations/")
    suspend fun getCourseRecommendations(): Response<ArrayList<InboxRecommendedCourse>>

    @POST("$DIR/payment/branch_log/")
    suspend fun savePaymentLog(@Body params: Map<String, Any>): Response<Void>

    @POST("$DIR/payment/juspay_log/")
    suspend fun saveJuspayPaymentLog(@Body params: Map<String, Any>): Response<Void>

    @POST("$DIR/micro_payment/user_wallet/")
    suspend fun deductAmountAfterCall(@Body params: Map<String, String>): Response<WalletBalance>

    @GET("$DIR/course/get_user_coupons/")
    suspend fun getValidCoupon(
        @Query("test_id") testId: Int,
        @Query("screen_name") screenName: String? = null,
        @Query("lessons_completed") lessonsCompleted: Int? = null
    ): Response<CouponListModel>

}
