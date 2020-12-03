package com.joshtalks.joshskills.repository.service

import com.joshtalks.joshskills.repository.local.model.GaIDMentorModel
import com.joshtalks.joshskills.repository.local.model.RequestRegisterGAId
import com.joshtalks.joshskills.repository.local.model.UserPlivoDetailsModel
import com.joshtalks.joshskills.repository.local.model.nps.NPSQuestionModel
import com.joshtalks.joshskills.repository.server.BaseResponse
import com.joshtalks.joshskills.repository.server.CertificateDetail
import com.joshtalks.joshskills.repository.server.ComplaintResponse
import com.joshtalks.joshskills.repository.server.FAQ
import com.joshtalks.joshskills.repository.server.FAQCategory
import com.joshtalks.joshskills.repository.server.FreshChatRestoreIDResponse
import com.joshtalks.joshskills.repository.server.LeaderboardResponse
import com.joshtalks.joshskills.repository.server.NPSByUserRequest
import com.joshtalks.joshskills.repository.server.RequestCertificateGenerate
import com.joshtalks.joshskills.repository.server.RequestComplaint
import com.joshtalks.joshskills.repository.server.SuccessResponse
import com.joshtalks.joshskills.repository.server.UserProfileResponse
import com.joshtalks.joshskills.repository.server.certification_exam.CertificateExamReportModel
import com.joshtalks.joshskills.repository.server.certification_exam.CertificationQuestionModel
import com.joshtalks.joshskills.repository.server.certification_exam.RequestSubmitCertificateExam
import com.joshtalks.joshskills.repository.server.conversation_practice.ConversationPractiseModel
import com.joshtalks.joshskills.repository.server.conversation_practice.SubmitConversationPractiseRequest
import com.joshtalks.joshskills.repository.server.conversation_practice.SubmittedConversationPractiseModel
import com.joshtalks.joshskills.repository.server.course_detail.CourseDetailsResponseV2
import com.joshtalks.joshskills.repository.server.feedback.FeedbackStatusResponse
import com.joshtalks.joshskills.repository.server.feedback.RatingDetails
import com.joshtalks.joshskills.repository.server.feedback.UserFeedbackRequest
import com.joshtalks.joshskills.repository.server.onboarding.CourseEnrolledRequest
import com.joshtalks.joshskills.repository.server.onboarding.CourseEnrolledResponse
import com.joshtalks.joshskills.repository.server.onboarding.VersionResponse
import com.joshtalks.joshskills.repository.server.points.PointsHistoryResponse
import com.joshtalks.joshskills.repository.server.points.PointsInfoResponse
import com.joshtalks.joshskills.repository.server.reminder.DeleteReminderRequest
import com.joshtalks.joshskills.repository.server.reminder.ReminderRequest
import com.joshtalks.joshskills.repository.server.reminder.ReminderResponse
import com.joshtalks.joshskills.repository.server.voip.RequestVoipRating
import com.joshtalks.joshskills.repository.server.voip.SpeakingTopicModel
import com.joshtalks.joshskills.repository.server.voip.VoipCallDetailModel
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
import java.util.HashMap

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
        @Query("interval_type") interval: String
    ): Response<LeaderboardResponse>

    @POST("$DIR/version/onboarding/")
    suspend fun getOnBoardingVersionDetails(@Body params: Map<String, String>): VersionResponse

    @GET("$DIR/voicecall/details")
    suspend fun getPlivoUserDetails(): UserPlivoDetailsModel

    @POST("$DIR/voicecall/feedback")
    suspend fun feedbackVoipCallAsync(@Body request: RequestVoipRating): Any

    @GET("$DIR/voicecall/topic/{id}/")
    suspend fun getTopicDetail(@Path("id") id: String): SpeakingTopicModel

    @GET("$DIR/voicecall/recipient_mentor")
    suspend fun getP2PUser(
        @Query("course_id") id: String,
        @Query("topic_id") topicId: Int?,
        @Query("support_user") supportUser: String
    ): VoipCallDetailModel

    @GET("$DIR/voicecall/mentor_topicinfo")
    suspend fun callMentorInfo(@Query("mobileuuid") id: String): HashMap<String, String?>


    @GET("$DIR/certificateexam/{id}/")
    suspend fun getCertificateExamDetails(@Path("id") id: Int): CertificationQuestionModel

    @POST("$DIR/certificateexam/report")
    suspend fun submitExam(@Body params: RequestSubmitCertificateExam): Response<Any>

    @GET("$DIR/certificateexam/report")
    suspend fun getExamReports(@Query("certificateexam_id") id: Int): List<CertificateExamReportModel>

    @GET("$DIR/group/user_profile/{mentor_id}/")
    suspend fun getUserProfileData(
        @Path("mentor_id") id: String
    ): Response<UserProfileResponse>

    @GET("$DIR/reputation/get_points_history/")
    suspend fun getUserPointsHistory(
        @Query("mentor_id") id: String
    ): Response<PointsHistoryResponse>

    @GET("$DIR/reputation/get_points_working/")
    suspend fun getPointsInfo(): Response<PointsInfoResponse>


    @FormUrlEncoded
    @PATCH("$DIR/voicecall/recipient_mentor")
    suspend fun postCallInitAsync(@FieldMap params: Map<String, String?>): Any

    @POST("$DIR/mentor/delete_mentor/")
    suspend fun deleteMentor(@Body params: Map<String, String>)
}
