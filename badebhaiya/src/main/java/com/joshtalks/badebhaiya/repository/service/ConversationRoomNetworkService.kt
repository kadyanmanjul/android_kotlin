package com.joshtalks.badebhaiya.repository.service

import com.joshtalks.badebhaiya.feed.model.*
import com.joshtalks.badebhaiya.impressions.Impression
import com.joshtalks.badebhaiya.liveroom.heartbeat.Heartbeat
import com.joshtalks.badebhaiya.profile.request.ReminderRequest
import com.joshtalks.badebhaiya.profile.request.DeleteReminderRequest
import com.joshtalks.badebhaiya.repository.model.ApiResponse
import com.joshtalks.badebhaiya.repository.model.ConversationRoomRequest
import com.joshtalks.badebhaiya.repository.model.ConversationRoomResponse
import com.joshtalks.badebhaiya.repository.model.PubNubExceptionRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface ConversationRoomNetworkService {

    @POST("$DIR/conversation_room/create_room/")
    suspend fun createRoom(@Body conversationRoomRequest: ConversationRoomRequest): Response<ConversationRoomResponse>

    @POST("$DIR/conversation_room/join_room/")
    suspend fun joinRoom(@Body conversationRoomRequest: ConversationRoomRequest): Response<ConversationRoomResponse>

    @POST("$DIR/conversation_room/leave_room/")
    suspend fun leaveRoom(@Body conversationRoomRequest: ConversationRoomRequest): Response<ApiResponse>

    @POST("$DIR/conversation_room/end_room/")
    suspend fun endRoom(@Body conversationRoomRequest: ConversationRoomRequest): Response<ApiResponse>

    @GET("$DIR/conversation_room/live_room_list/")
    suspend fun getRoomList(): Response<RoomListResponse>

    @POST("$DIR/reminder/set_reminder/")
    suspend fun setReminder(@Body reminderRequest: ReminderRequest): Response<Void>

    @POST("$DIR/conversation_room/schedule_room/")
    suspend fun scheduleRoom(@Body scheduleRequest: ConversationRoomRequest): Response<RoomListResponseItem>

    @POST("$DIR/reminder/delete_reminder/")
    suspend fun deleteReminder(@Body deleteReminderRequest: DeleteReminderRequest): Response<Void>

    @POST("$DIR/conversation_room/search/")
    suspend fun searchRoom(@Body parems:Map<String,String>):Response<SearchRoomsResponseList>

    @GET("$DIR/user/speakers_to_follow/?page=1")
    suspend fun speakersList(page: Int):Response<List<Users>>

    @POST("$DIR/conversation_room/pubnub_exception/")
//    @POST("http://bbapp-prod.joshtalks.org/api/bbapp/v1/conversation_room/pubnub_exception/")
    suspend fun sendPubNubException(@Body params: PubNubExceptionRequest): Response<*>

    @GET("$DIR/user/waiting_room_users/")
    suspend fun waitingMember():Response<WaitingList>

    @POST("$DIR/impressions/track_impressions/")
    suspend fun sendEvent(@Body event: Impression):Response<Void>

    @POST("$DIR/conversation_room/live_room_users/")
    suspend fun triggerHeartbeat(@Body body: Heartbeat):Response<Void>

}