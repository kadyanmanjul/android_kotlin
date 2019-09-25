package com.joshtalks.joshskills.repository.service

import com.joshtalks.joshskills.repository.local.entity.ChatModel
import com.joshtalks.joshskills.repository.local.entity.Course
import com.joshtalks.joshskills.repository.server.AmazonPolicyResponse
import com.joshtalks.joshskills.repository.server.ChatMessageReceiver
import com.joshtalks.joshskills.repository.server.ResponseChatMessage
import com.joshtalks.joshskills.repository.server.chat_message.BaseChatMessage
import com.joshtalks.joshskills.repository.server.chat_message.TChatMessage
import kotlinx.coroutines.Deferred
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.*


@JvmSuppressWildcards
interface ChatNetworkService {

    @GET("$DIR/course/")
    fun getRegisterCourses(): Deferred<List<Course>>

    @POST("$DIR/chat/message/")
    fun sendMessage(@Body messageObject: Any): Deferred<ChatMessageReceiver>

    @GET("$DIR/chat/{id}/")
    fun getUnReceivedMessageAsync(@Path("id") id: String, @QueryMap params: Map<String, String>): Deferred<ResponseChatMessage>


    @GET
    fun downloadFileAsync(@Url fileUrl: String): Deferred<Any>

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


}
