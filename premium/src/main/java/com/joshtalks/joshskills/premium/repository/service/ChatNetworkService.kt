package com.joshtalks.joshskills.premium.repository.service

import com.joshtalks.joshskills.base.constants.DIR
import com.joshtalks.joshskills.premium.repository.local.entity.*
import com.joshtalks.joshskills.premium.repository.local.entity.practise.PointsListResponse
import com.joshtalks.joshskills.premium.repository.local.entity.practise.PracticeEngagementV2
import com.joshtalks.joshskills.premium.repository.server.*
import com.joshtalks.joshskills.premium.repository.server.assessment.*
import com.joshtalks.joshskills.premium.repository.server.chat_message.UpdateQuestionStatus
import com.joshtalks.joshskills.premium.repository.server.course_overview.CourseOverviewBaseResponse
import com.joshtalks.joshskills.premium.repository.server.introduction.DemoOnboardingData
import com.joshtalks.joshskills.premium.ui.lesson.speaking.spf_models.BlockStatusModel
import com.joshtalks.joshskills.premium.ui.lesson.speaking.spf_models.UserRating
import com.joshtalks.joshskills.premium.ui.lesson.speaking.spf_models.VideoPopupItem
import com.joshtalks.joshskills.premium.calling.data.api.CallRecordingRequest
import kotlinx.coroutines.Deferred
import retrofit2.Response
import retrofit2.http.*

@JvmSuppressWildcards
interface ChatNetworkService {

    @GET("$DIR/course/")
    suspend fun getRegisteredCourses(): List<Course>

    @POST("$DIR/chat/message/")
    suspend fun sendMessageAsync(@Body messageObject: Any): ChatMessageReceiver

    @GET("$DIR/chat/v2/{id}/")
    suspend fun getUnReceivedMessageAsync(
        @Path("id") id: String,
        @QueryMap params: Map<String, String>
    ): ResponseChatMessage

    @GET("$DIR/chat/schedule_message/")
    suspend fun scheduleMessage(
        @QueryMap params: Map<String, Any?>
    ): Response<Unit>

    @FormUrlEncoded
    @POST("$DIR/core/signed_url/")
    fun requestUploadMediaAsync(@FieldMap params: Map<String, String>): Deferred<AmazonPolicyResponse>

    @POST("$DIR/engage/video_course/")
    suspend fun engageVideoApiV2(@Body messageObject: Any)

    @POST("$DIR/engage/performance_video/")
    suspend fun engageSharableVideoApi(@Body messageObject: Any)

    @POST("$DIR/engage/audio/")
    suspend fun engageAudio(@Body messageObject: Any)

    @POST("$DIR/engage/pdf/")
    suspend fun engagePdf(@Body messageObject: Any)

    @POST("$DIR/engage/image/")
    suspend fun engageImage(@Body messageObject: Any)

    @POST("$DIR/practice/engagement/")
    suspend fun submitPracticeAsync(@Body requestEngage: RequestEngage): Response<PracticeEngagement>

    @POST("$DIR/practice/engagement/")
    suspend fun submitNewReadingPractice(@Body requestEngage: RequestEngage): Response<PracticeEngagementV2>

    @POST("$DIR/practice/audio_practice_feedback/")
    suspend fun getAudioFeedback(@Body params: Map<String, String>): PracticeFeedback2

    @GET("$DIR/chat/conversation/{id}/")
    suspend fun getCourseProgressDetailsAsync(@Path("id") cId: String): Response<CoursePerformanceResponse>

    @GET("$DIR/assessment/{id}/")
    suspend fun getAssessmentId(
        @Path("id") id: Int
    ): Response<AssessmentResponse>

    @POST("$DIR/assessment/response/")
    suspend fun submitTestAsync(
        @Body assessmentRequest: AssessmentRequest
    )

    @GET("$DIR/assessment/report/{id}/")
    suspend fun getTestReport(
        @Path("id") id: Int
    ): Response<AssessmentResponse>

    @PATCH("$DIR/chat/add_next_class/{id}/")
    suspend fun changeBatchRequest(@Path("id") conversationId: String): Response<Void>

    @GET("$DIR/chat/v2/lesson_questions/")
    suspend fun getQuestionsForLesson(
        @Query("last_question_time") latestModifiedTime: String,
        @Query("lesson_id") lessonId: Int
    ): GetLessonQuestionsResponse

    @POST("$DIR/chat/v2/update_lesson/")
    suspend fun updateQuestionStatus(
        @Body questionStatus: UpdateQuestionStatus
    ): Response<UpdateLessonResponse>

    @GET("$DIR/course/course_overview/")
    suspend fun getCourseOverview(
        @Query("mentor_id") mentorId: String,
        @Query("course_id") courseId: Int
    ): CourseOverviewBaseResponse

    @GET("$DIR/certificateexam/chatcard-report")
    suspend fun getCertificateExamCardDetails(@QueryMap params: Map<String, String>): CertificationExamDetailModel

    @GET("$DIR/reputation/vp_rp_snackbar/")
    suspend fun getSnackBarText(
        @Query("question_id") questionId: String? = null,
        @Query("channel_name") channelName: String? = null,
        @Query("room_id") roomId: String? = null,
        @Query("conversation_question_id") conversationQuestionId: String? = null,
    ): PointsListResponse

    @GET("$DIR/course/get_demo_lesson/")
    suspend fun getDemoLessonModel(): LessonModel

    @Headers(
        "Accept: application/json",
        "Content-type:application/json",
        "Cache-Control: public, only-if-cached,  max-stale=640000,  max-age=640000"
    )
    @GET("$DIR/course/demo_onboarding_data/")
    suspend fun getDemoOnBoardingData(): Response<DemoOnboardingData>

    @GET("$DIR/course/introduction_data/")
    suspend fun getIntroSpeakingVideo(): Response<VideoPopupItem>

    @GET("$DIR/assessment/test_v4/")
    suspend fun getOnlineTestQuestion(@QueryMap params: Map<String, Int>): Response<OnlineTestResponse>

    @POST("$DIR/assessment/test_v4/")
    suspend fun postAndGetNextOnlineTestQuestion(@Body onlineTestRequest: OnlineTestRequest): Response<OnlineTestResponse>

    @GET("$DIR/assessment/rule/")
    suspend fun getListOfRuleIds(): Response<RuleIdsList>

    @POST("$DIR/assessment/rule/")
    suspend fun setListOfRuleIdsCompleted(@Body params: Map<String, Int>)

    @PATCH("$DIR/course/extend_free_trial/")
    suspend fun extendFreeTrial(
        @Body id: Map<String, String>
    ): Response<Any>

    @POST("$DIR/impression/track_a2c1_retention_impression/")
    suspend fun saveA2C1Impression(@Body requestData: HashMap<String, String>)

    @Headers("Cache-Control: public, only-if-cached,  max-stale=86400,  max-age=86400")
    @GET("$DIR/p2p/rating/")
    suspend fun getUserRating(): Response<UserRating>

    @POST("$DIR/voicecall/agora_call_share")
    suspend fun postCallRecordingFile(@Body request : CallRecordingRequest) : Response<Unit>

    @GET("$DIR/p2p/block_status")
    suspend fun getUserBlockStatus(): Response<BlockStatusModel>
}
