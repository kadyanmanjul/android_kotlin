package com.joshtalks.joshskills.repository.service

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.joshtalks.joshskills.repository.local.entity.CertificationExamDetailModel
import com.joshtalks.joshskills.repository.local.entity.Course
import com.joshtalks.joshskills.repository.local.entity.GetLessonQuestionsResponse
import com.joshtalks.joshskills.repository.local.entity.LessonModel
import com.joshtalks.joshskills.repository.local.entity.PracticeEngagement
import com.joshtalks.joshskills.repository.local.entity.PracticeFeedback2
import com.joshtalks.joshskills.repository.local.entity.practise.PointsListResponse
import com.joshtalks.joshskills.repository.local.entity.practise.PracticeEngagementV2
import com.joshtalks.joshskills.repository.server.AmazonPolicyResponse
import com.joshtalks.joshskills.repository.server.BaseResponse
import com.joshtalks.joshskills.repository.server.ChatMessageReceiver
import com.joshtalks.joshskills.repository.server.CoursePerformanceResponse
import com.joshtalks.joshskills.repository.server.RequestEngage
import com.joshtalks.joshskills.repository.server.ResponseChatMessage
import com.joshtalks.joshskills.repository.server.UpdateLessonResponse
import com.joshtalks.joshskills.repository.server.assessment.AssessmentRequest
import com.joshtalks.joshskills.repository.server.assessment.AssessmentResponse
import com.joshtalks.joshskills.repository.server.chat_message.UpdateQuestionStatus
import com.joshtalks.joshskills.repository.server.course_overview.CourseOverviewBaseResponse
import com.joshtalks.joshskills.repository.server.groupchat.GroupDetails
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

@JvmSuppressWildcards
interface ChatNetworkService {

    @GET("$DIR/course/")
    suspend fun getRegisteredCourses(): List<Course>

    @POST("$DIR/chat/message/")
    suspend fun sendMessageAsync(@Body messageObject: Any): ChatMessageReceiver

    @PATCH("$DIR/chat/message/{id}")
    suspend fun deleteMessage(
        @Path("id") id: String,
        @FieldMap params: Map<String, String>
    ): ChatMessageReceiver

    @GET("$DIR/chat/v2/{id}/")
    suspend fun getUnReceivedMessageAsync(
        @Path("id") id: String,
        @QueryMap params: Map<String, String>
    ): ResponseChatMessage

    @FormUrlEncoded
    @POST("$DIR/core/signed_url/")
    fun requestUploadMediaAsync(@FieldMap params: Map<String, String>): Deferred<AmazonPolicyResponse>

    @POST("$DIR/engage/video/")
    suspend fun engageVideo(@Body messageObject: Any)

    @POST("$DIR/engage/video_course/")
    suspend fun engageVideoApiV2(@Body messageObject: Any)


    @POST("$DIR/engage/audio/")
    suspend fun engageAudio(@Body messageObject: Any)

    @POST("$DIR/engage/pdf/")
    suspend fun engagePdf(@Body messageObject: Any)

    @POST("$DIR/engage/image/")
    suspend fun engageImage(@Body messageObject: Any)

    @FormUrlEncoded
    @PATCH("$DIR/notification/{id}/")
    suspend fun engageNotificationAsync(
        @Path("id") id: String,
        @FieldMap params: Map<String, String>
    )

    @PATCH("$DIR/chat/message/list/")
    suspend fun updateMessagesStatus(@Body messageObject: Any)

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

    @GET("$DIR/chat/lessons/")
    suspend fun getLessonList(
        @Query("mentor_id") mentorId: String,
        @Query("course_id") courseId: String
    ): BaseResponse<List<LessonModel>>

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

    @POST("$DIR/group/cometchat_add_member/")
    suspend fun getGroupDetails(@Body params: Map<String, String>): GroupDetails

    @GET("$DIR/certificateexam/chatcard-report")
    suspend fun getCertificateExamCardDetails(@QueryMap params: Map<String, String>): CertificationExamDetailModel

    @POST("$DIR/group/message_list/")
    suspend fun getGroupMessagesList(@Body params: Map<String, Any>): Response<JsonArray>

    @POST("$DIR/group/updatelastmessage/")
    suspend fun updateLastReadMessage(@Body params: Map<String, Any>): Response<JsonObject>

    @GET("$DIR/group/{conversation_id}/unread_message/ ")
    suspend fun getUnreadMessageCount(
        @Path("conversation_id") conversationId: String
    ): Response<JsonObject>

    @GET("$DIR/reputation/vp_rp_snackbar/")
    suspend fun getSnackBarText(
        @Query("question_id") questionId: String?,
        @Query("channel_name") channelName: String?=null,
    ): PointsListResponse

    @GET("$DIR/course/get_demo_lesson/")
    suspend fun getDemoLessonModel(): LessonModel

}
