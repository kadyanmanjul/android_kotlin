package com.joshtalks.joshskills.repository.service

import com.joshtalks.joshskills.repository.local.entity.Course
import com.joshtalks.joshskills.repository.local.entity.LessonModel
import com.joshtalks.joshskills.repository.local.entity.PracticeEngagement
import com.joshtalks.joshskills.repository.local.entity.PracticeFeedback2
import com.joshtalks.joshskills.repository.local.entity.Question
import com.joshtalks.joshskills.repository.server.AmazonPolicyResponse
import com.joshtalks.joshskills.repository.server.BaseResponse
import com.joshtalks.joshskills.repository.server.ChatMessageReceiver
import com.joshtalks.joshskills.repository.server.CoursePerformanceResponse
import com.joshtalks.joshskills.repository.server.RequestEngage
import com.joshtalks.joshskills.repository.server.ResponseChatMessage
import com.joshtalks.joshskills.repository.server.assessment.AssessmentRequest
import com.joshtalks.joshskills.repository.server.assessment.AssessmentResponse
import com.joshtalks.joshskills.repository.server.chat_message.UpdateQuestionStatus
import com.joshtalks.joshskills.repository.server.course_overview.CourseOverviewResponse
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
    suspend fun getRegisterCourses(): Response<List<Course>>

    @POST("$DIR/chat/message/")
    fun sendMessageAsync(@Body messageObject: Any): Deferred<ChatMessageReceiver>

    @PATCH("$DIR/chat/message/{id}")
    suspend fun deleteMessage(
        @Path("id") id: String,
        @FieldMap params: Map<String, String>
    ): ChatMessageReceiver

    @GET("$DIR/chat/{id}/")
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

    //http://staging.joshtalks.org/api/skill/v1/practice/audio_practice_feedback/'
    @POST("$DIR/practice/audio_practice_feedback/")
    suspend fun getAudioFeedback(@Body params: Map<String, String>): PracticeFeedback2

    @FormUrlEncoded
    @PATCH("$DIR/mentor/gaid/{id}/")
    suspend fun mergeMentorWithGAId(@Path("id") id: String, @FieldMap params: Map<String, String>)

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
    suspend fun changeBatchRequest(@Path("id") conversationId: String): Response<Any>

    @GET("$DIR/chat/lessons/")
    suspend fun getLessonList(
        @Query("mentor_id") mentorId: String,
        @Query("course_id") courseId: String
    ): BaseResponse<List<LessonModel>>

    @GET("$DIR/chat/lesson_questions/")
    suspend fun getQuestionsForLesson(
        @Query("mentor_id") mentorId: String,
        @Query("lesson_id") lessonId: Int
    ): BaseResponse<List<Question>>


    @POST("$DIR/chat/update_lesson/")
    suspend fun updateQuestionStatus(
        @Body questionStatus: UpdateQuestionStatus
    )


    @GET("$DIR/course/course_overview/")
    suspend fun getCourseOverview(
        @Query("mentor_id") mentorId: String,
        @Query("course_id") courseId: String
    ): BaseResponse<List<CourseOverviewResponse>>


}
