package com.joshtalks.badebhaiya.repository

import com.joshtalks.badebhaiya.profile.request.ReminderRequest
import com.joshtalks.badebhaiya.repository.model.ConversationRoomRequest
import com.joshtalks.badebhaiya.repository.service.ConversationRoomNetworkService
import com.joshtalks.badebhaiya.repository.service.RetrofitInstance

class ConversationRoomRepository {

    val service: ConversationRoomNetworkService = RetrofitInstance.conversationRoomNetworkService


    suspend fun getRoomList() =
        service.getRoomList()

    suspend fun createRoom(conversationRoomRequest: ConversationRoomRequest) =
        service.createRoom(conversationRoomRequest)

    suspend fun joinRoom(conversationRoomRequest: ConversationRoomRequest) =
        service.joinRoom(conversationRoomRequest)

    suspend fun leaveRoom(conversationRoomRequest: ConversationRoomRequest) =
        service.leaveRoom(conversationRoomRequest)

    suspend fun endRoom(conversationRoomRequest: ConversationRoomRequest) =
        service.endRoom(conversationRoomRequest)

    suspend fun setReminder(reminderRequest: ReminderRequest) =
        service.setReminder(reminderRequest)
}