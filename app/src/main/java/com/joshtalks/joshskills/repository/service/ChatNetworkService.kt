package com.joshtalks.joshskills.repository.service

import com.joshtalks.joshskills.repository.local.entity.Course
import com.joshtalks.joshskills.repository.local.entity.PracticeEngagement
import com.joshtalks.joshskills.repository.server.*
import kotlinx.coroutines.Deferred
import retrofit2.Response
import retrofit2.http.*

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

    @FormUrlEncoded
    @PATCH("$DIR/mentor/gaid/{id}/")
    suspend fun mergeMentorWithGId(@Path("id") id: String, @FieldMap params: Map<String, String>)

    @GET("$DIR/chat/conversation/{id}/")
    suspend fun getCourseProgressDetailsAsync(@Path("id") cId: String): Response<CoursePerformanceResponse>



}
