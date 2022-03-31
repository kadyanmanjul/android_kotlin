package com.joshtalks.badebhaiya.repository.service

import com.joshtalks.badebhaiya.feed.model.RoomListResponse
import com.joshtalks.badebhaiya.repository.model.ApiResponse
import com.joshtalks.badebhaiya.repository.model.ConversationRoomRequest
import com.joshtalks.badebhaiya.repository.model.ConversationRoomResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface ConversationRoomNetworkService {

    @POST("$DIR/conversation_room/create_room")
    suspend fun createRoom(@Body conversationRoomRequest: ConversationRoomRequest): Response<ConversationRoomResponse>

    @POST("$DIR/conversation_room/join_room")
    suspend fun joinRoom(@Body conversationRoomRequest: ConversationRoomRequest): Response<ConversationRoomResponse>

    @POST("$DIR/conversation_room/leave_room")
    suspend fun leaveRoom(@Body conversationRoomRequest: ConversationRoomRequest): Response<ApiResponse>

    @POST("$DIR/conversation_room/end_room")
    suspend fun endRoom(@Body conversationRoomRequest: ConversationRoomRequest): Response<ApiResponse>

    @GET("$DIR/conversation_room/live_room_list/")
    suspend fun getRoomList(): Response<RoomListResponse>

}