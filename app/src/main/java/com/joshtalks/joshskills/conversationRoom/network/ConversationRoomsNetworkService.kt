package com.joshtalks.joshskills.conversationRoom.network

import com.joshtalks.joshskills.conversationRoom.model.ConversationRoomResponse
import com.joshtalks.joshskills.conversationRoom.model.ConversionLiveRoomExitResponse
import com.joshtalks.joshskills.conversationRoom.model.CreateConversionRoomRequest
import com.joshtalks.joshskills.conversationRoom.model.JoinConversionRoomRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

const val DIR = "api/skill/v1"

interface ConversationRoomsNetworkService {

    @POST("$DIR/conversation_room/create_room/")
    suspend fun createConversationRoom(@Body requestCreate: CreateConversionRoomRequest): Response<ConversationRoomResponse>

    @POST("$DIR/conversation_room/join_room/")
    suspend fun joinConversationRoom(@Body requestJoin: JoinConversionRoomRequest): Response<ConversationRoomResponse>

    @POST("$DIR/conversation_room/leave_room/")
    suspend fun leaveConversationLiveRoom(@Body requestJoin: JoinConversionRoomRequest): Response<ConversionLiveRoomExitResponse>

    @POST("$DIR/conversation_room/end_room/")
    suspend fun endConversationLiveRoom(@Body requestJoin: JoinConversionRoomRequest): Response<ConversionLiveRoomExitResponse>
}