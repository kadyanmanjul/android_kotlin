package com.joshtalks.joshskills.repository.service

import com.joshtalks.joshskills.repository.local.entity.Course
import com.joshtalks.joshskills.repository.local.entity.PracticeEngagement
import com.joshtalks.joshskills.repository.server.AmazonPolicyResponse
import com.joshtalks.joshskills.repository.server.ChatMessageReceiver
import com.joshtalks.joshskills.repository.server.RequestEngage
import com.joshtalks.joshskills.repository.server.ResponseChatMessage
import io.reactivex.subjects.ReplaySubject
import kotlinx.coroutines.Deferred
import retrofit2.http.*

@JvmSuppressWildcards
interface ChatNetworkService {

    @GET("$DIR/course/")
    suspend fun getRegisterCourses(): List<Course>

    @POST("$DIR/chat/message/")
    fun sendMessage(@Body messageObject: Any): Deferred<ChatMessageReceiver>

    @PATCH("$DIR/chat/message/{id}")
    suspend fun deleteMessage(@Path("id") id: String, @FieldMap params: Map<String, String>): ChatMessageReceiver

    @GET("$DIR/chat/{id}/")
    suspend fun getUnReceivedMessageAsync(@Path("id") id: String, @QueryMap params: Map<String, String>): ResponseChatMessage

    @FormUrlEncoded
    @POST("$DIR/core/signed_url/")
    fun requestUploadMediaAsync(@FieldMap params: Map<String, String>): Deferred<AmazonPolicyResponse>

    @POST("$DIR/engage/video/")
    suspend fun engageVideo(@Body messageObject: Any)

    @POST("$DIR/engage/audio/")
    suspend fun engageAudio(@Body messageObject: Any)

    @POST("$DIR/engage/pdf/")
    suspend fun engagePdf(@Body messageObject: Any)

    @POST("$DIR/engage/image/")
    suspend fun engageImage(@Body messageObject: Any)

    @FormUrlEncoded
    @PATCH("$DIR/notification/{id}/")
    suspend fun engageNotificationAsync(@Path("id") id: String, @FieldMap params: Map<String, String>)

    @PATCH("$DIR/chat/message/list/")
    suspend fun updateMessagesStatus(@Body messageObject: Any)


    @POST("$DIR/practice/engagement/")
    fun submitPracticeAsync(@Body requestEngage: RequestEngage): Deferred<PracticeEngagement>

    @POST("$DIR/practice/engagement/")
    fun dwd(): ReplaySubject<String>


}
