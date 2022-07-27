package com.joshtalks.badebhaiya.repository

import com.joshtalks.badebhaiya.impressions.Impression
import com.joshtalks.badebhaiya.impressions.Records
import com.joshtalks.badebhaiya.profile.request.DeleteReminderRequest
import com.joshtalks.badebhaiya.profile.request.ReminderRequest
import com.joshtalks.badebhaiya.repository.model.ConversationRoomRequest
import com.joshtalks.badebhaiya.repository.service.ConversationRoomNetworkService
import com.joshtalks.badebhaiya.repository.service.RetrofitInstance

class ConversationRoomRepository {

    val service: ConversationRoomNetworkService = RetrofitInstance.conversationRoomNetworkService

    suspend fun getRoomList() =
        service.getRoomList()

    suspend fun getRecordsList()=service.getRecordsList()

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

    suspend fun scheduleRoom(scheduleRequest: ConversationRoomRequest) = service.scheduleRoom(scheduleRequest)

    suspend fun deleteReminder(deleteReminderRequest: DeleteReminderRequest)=
        service.deleteReminder(deleteReminderRequest)

    suspend fun searchRoom(parems:Map<String,String>)=
        service.searchRoom(parems)

    suspend fun speakersList(page:Int)=
        service.speakersList(page)

    suspend fun waitingList()=service.waitingMember()

    suspend fun sendEvent(impression:Impression)=service.sendEvent(impression)

    suspend fun requestUploadRoomRecording( record:Records)=service.requestUploadRoomRecording(record)

}