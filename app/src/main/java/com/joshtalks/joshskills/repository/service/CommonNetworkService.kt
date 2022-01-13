package com.joshtalks.joshskills.repository.service

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.joshtalks.joshskills.engage_notification.AppUsageModel
import com.joshtalks.joshskills.repository.local.model.GaIDMentorModel
import com.joshtalks.joshskills.repository.local.model.RequestRegisterGAId
import com.joshtalks.joshskills.repository.local.model.nps.NPSQuestionModel
import com.joshtalks.joshskills.repository.server.*
import com.joshtalks.joshskills.repository.server.certification_exam.CertificateExamReportModel
import com.joshtalks.joshskills.repository.server.certification_exam.CertificationQuestionModel
import com.joshtalks.joshskills.repository.server.certification_exam.CertificationUserDetail
import com.joshtalks.joshskills.repository.server.certification_exam.RequestSubmitCertificateExam
import com.joshtalks.joshskills.repository.server.conversation_practice.ConversationPractiseModel
import com.joshtalks.joshskills.repository.server.conversation_practice.SubmitConversationPractiseRequest
import com.joshtalks.joshskills.repository.server.conversation_practice.SubmittedConversationPractiseModel
import com.joshtalks.joshskills.repository.server.course_detail.CourseDetailsResponseV2
import com.joshtalks.joshskills.repository.server.course_detail.demoCourseDetails.DemoCourseDetailsResponse
import com.joshtalks.joshskills.repository.server.feedback.FeedbackStatusResponse
import com.joshtalks.joshskills.repository.server.feedback.RatingDetails
import com.joshtalks.joshskills.repository.server.feedback.UserFeedbackRequest
import com.joshtalks.joshskills.repository.server.onboarding.CourseEnrolledRequest
import com.joshtalks.joshskills.repository.server.onboarding.CourseEnrolledResponse
import com.joshtalks.joshskills.repository.server.onboarding.VersionResponse
import com.joshtalks.joshskills.repository.server.points.PointsHistoryResponse
import com.joshtalks.joshskills.repository.server.points.PointsInfoResponse
import com.joshtalks.joshskills.repository.server.points.SpokenMinutesHistoryResponse
import com.joshtalks.joshskills.repository.server.reminder.DeleteReminderRequest
import com.joshtalks.joshskills.repository.server.reminder.ReminderRequest
import com.joshtalks.joshskills.repository.server.reminder.ReminderResponse
import com.joshtalks.joshskills.repository.server.translation.WordDetailsResponse
import com.joshtalks.joshskills.repository.server.voip.RequestVoipRating
import com.joshtalks.joshskills.repository.server.voip.SpeakingTopic
import com.joshtalks.joshskills.repository.server.voip.VoipCallDetailModel
import com.joshtalks.joshskills.track.CourseUsageSync
import kotlinx.coroutines.Deferred
import retrofit2.Response
import retrofit2.http.*

@JvmSuppressWildcards
interface CommonNetworkService {

    @GET("$DIR/support/category_v2/")
    suspend fun getHelpCategoryV2(): Response<List<FAQCategory>>

    @POST("$DIR/support/complaint/")
    suspend fun submitComplaint(@Body requestComplaint: RequestComplaint): ComplaintResponse

    @POST("$DIR/mentor/gaid/")
    fun registerGAIdAsync(@Body requestRegisterGAId: RequestRegisterGAId): Deferred<RequestRegisterGAId>

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
    suspend fun patchMentorWithGAIdAsync(
        @Path("id") id: Int,
        @Body params: HashMap<String, @JvmSuppressWildcards List<String>>
    ): Response<Any>

    @FormUrlEncoded
    @PATCH("$DIR/mentor/gaid/{id}/")
    suspend fun mergeMentorWithGAId(@Path("id") id: String, @FieldMap params: Map<String, String>)

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

    @GET("$DIR/course/course_details_v2/")
    suspend fun getDemoCourseDetails(): Response<DemoCourseDetailsResponse>

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

    @GET("$DIR/mentor/reminders/")
    suspend fun getReminders(@Query("mentor_id") mentorId: String): BaseResponse<List<ReminderResponse>>

    @POST("$DIR/mentor/delete_reminders/")
    suspend fun deleteReminders(@Body deleteReminderRequest: DeleteReminderRequest): Response<BaseResponse<*>>

    @GET("$DIR/mentor/voicecall/initiate/")
    suspend fun voipInitDetails(@QueryMap params: Map<String, String>): VoipCallDetailModel

    @GET("$DIR/leaderboard/get_leaderboard/")
    suspend fun getLeaderBoardData(
        @Query("mentor_id") mentorId: String,
        @Query("interval_type") interval: String,
        @Query("course_id") course_id: String?
    ): Response<LeaderboardResponse>

    // not using this
    @GET("$DIR/leaderboard/get_leaderboard/")
    suspend fun getLeaderBoardDataViaPage(
        @Query("mentor_id") mentorId: String,
        @Query("interval_type") interval: String,
        @Query("above_list_page") page: Int
    ): Response<LeaderboardResponse>

    @GET("$DIR/leaderboard/get_animated_leaderboard/")
    suspend fun getAnimatedLeaderBoardData(
        @Query("mentor_id") mentorId: String
    ): Response<AnimatedLeaderBoardResponse>

    @POST("$DIR/version/onboarding/")
    suspend fun getOnBoardingVersionDetails(@Body params: Map<String, String>): VersionResponse

    @POST("$DIR/voicecall/feedback")
    suspend fun feedbackVoipCallAsync(@Body request: RequestVoipRating): Response<FeedbackVoipResponse>

    @GET("$DIR/voicecall/topic/v2/{id}/")
    suspend fun getTopicDetail(@Path("id") id: String): SpeakingTopic

    @GET("$DIR/voicecall/recipient_mentor")
    suspend fun getP2PUser(@QueryMap params: Map<String, String?>): VoipCallDetailModel

    @GET("$DIR/voicecall/mentor_topicinfo")
    suspend fun callMentorInfo(@Query("mobileuuid") id: String): HashMap<String, String?>

    @GET("$DIR/certificateexam/{id}/")
    suspend fun getCertificateExamDetails(@Path("id") id: Int): CertificationQuestionModel

    @POST("$DIR/certificateexam/v2/report")
    suspend fun submitExam(@Body params: RequestSubmitCertificateExam): Response<Any>

    @GET("$DIR/certificateexam/report")
    suspend fun getExamReports(@Query("certificateexam_id") id: Int): List<CertificateExamReportModel>

    @GET("$DIR/certificateexam/user_details")
    suspend fun getCertificateUserDetails(): CertificationUserDetail?

    @POST("$DIR/certificateexam/user_details")
    suspend fun submitUserDetailForCertificate(@Body certificationUserDetail: CertificationUserDetail): Map<String, String>

    @GET("$DIR/group/user_profile_v2/{mentor_id}/")
    suspend fun getUserProfileData(
        @Path("mentor_id") id: String,
        @Query("interval_type") intervalType: String? = null,
        @Query("previous_page") previousPage: String? = null
    ): Response<UserProfileResponse>

    @GET("$DIR/reputation/get_points_history_v2/")
    suspend fun getUserPointsHistory(
        @Query("mentor_id") id: String
    ): Response<PointsHistoryResponse>

    @GET("$DIR/reputation/get_spoken_history_v2/")
    suspend fun getUserSpokenMinutesHistory(
        @Query("mentor_id") id: String
    ): Response<SpokenMinutesHistoryResponse>

    @Headers(
        "Accept: application/json",
        "Content-type:application/json",
        "Cache-Control: public, only-if-cached,  max-stale=640000,  max-age=640000"
    )
    @GET("$DIR/reputation/get_points_working/")
    suspend fun getPointsInfo(): Response<PointsInfoResponse>

    @FormUrlEncoded
    @PATCH("$DIR/voicecall/recipient_mentor")
    suspend fun postCallInitAsync(@FieldMap params: Map<String, String?>): Any

    @POST("$DIR/mentor/delete_mentor/")
    suspend fun deleteMentor(@Body params: Map<String, String>): Response<Void>

    @GET("$DIR/group/{group_id}/pinnedmessages/")
    suspend fun getPinnedMessages(
        @Path("group_id") groupId: String
    ): Response<JsonArray>

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

    @POST("$DIR/leaderboard/leaderboard_impression/")
    suspend fun engageLeaderBoardImpressions(
        @Body params: Map<String, String>
    ): Any

    @GET("$DIR/reputation/award_render/")
    suspend fun get3DWebView(
        @Query("award_mentor_id") awardMentorId: String
    ): String

    @FormUrlEncoded
    @PUT("$DIR/group/voicenote/notification/")
    suspend fun audioPlayed(
        @Field("group_id") groupId: String,
        @Field("message_id") messageId: Int
    ): Response<Any>

    @POST("$DIR/engage/user-activity/")
    suspend fun engageUserSession(
        @Body params: HashMap<String, List<AppUsageModel>>
    ): Response<Void>

    @POST("$DIR/engage/course-user-activity/")
    suspend fun engageCourseUsageSession(
        @Body params: HashMap<String, List<CourseUsageSync>>
    ): Response<Void>

    @POST("$DIR/mentor/gaid/")
    suspend fun registerGAIdDetailsV2Async(@Body body: RequestRegisterGAId): GaIDMentorModel

    @POST("$DIR/group/updatelastmessage/")
    suspend fun updateLastReadMessage(@Body params: Map<String, Any>): Response<JsonObject>

    @GET("$DIR/group/{conversation_id}/unread_message/ ")
    suspend fun getUnreadMessageCount(
        @Path("conversation_id") conversationId: String
    ): Response<JsonObject>

    @GET("$DIR/leaderboard/get_filtered_leaderboard/")
    suspend fun searchLeaderboardMember(
        @Query("key") word: String,
        @Query("page") page: Int,
        @Query("interval_type") intervalType: LeaderboardType
    ): Response<List<LeaderboardMentor>>

    @GET("$DIR/leaderboard/get_previous_leaderboard/")
    suspend fun getPreviousLeaderboardData(
        @Query("mentor_id") mentorId: String,
        @Query("interval_type") intervalType: String
    ): Response<PreviousLeaderboardResponse>

    @POST("$DIR/course/free_trial_register_course/")
    suspend fun enrollFreeTrialMentorWithCourse(@Body params: Map<String, String>): Response<Void>

    @POST("$DIR/impression/track_impressions/")
    suspend fun saveImpression(@Body params: Map<String, String>): Response<Void>

    @POST("$DIR/link_attribution/deep_link/")
    suspend fun getDeepLink(@Body params: LinkAttribution): Response<Any>

    @POST("$DIR/impression/track_referral_impressions/")
    suspend fun saveReferralImpression(@Body params: Map<String, String>): Response<Void>
}
