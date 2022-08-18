package com.joshtalks.badebhaiya.repository

import android.util.Log
import com.joshtalks.badebhaiya.feed.model.RecordedResponseList
import com.joshtalks.badebhaiya.impressions.Impression
import com.joshtalks.badebhaiya.impressions.Records
import com.joshtalks.badebhaiya.impressions.UserRecords
import com.joshtalks.badebhaiya.profile.request.DeleteReminderRequest
import com.joshtalks.badebhaiya.profile.request.ReminderRequest
import com.joshtalks.badebhaiya.profile.response.ProfileResponse
import com.joshtalks.badebhaiya.repository.model.ConversationRoomRequest
import com.joshtalks.badebhaiya.repository.model.User
import com.joshtalks.badebhaiya.repository.service.ConversationRoomNetworkService
import com.joshtalks.badebhaiya.repository.service.RetrofitInstance
import retrofit2.Response

class ConversationRoomRepository {

    val service: ConversationRoomNetworkService = RetrofitInstance.conversationRoomNetworkService

    suspend fun getRoomList() =
        service.getRoomList()

    suspend fun getRecordsList(roomId: Int?=null)=service.getRecordList(roomId)

    suspend fun userRoomRecord(id:Int, user:String)=service.userRoomRecord(UserRecords(id, user))

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

     fun getSearchSuggestions() = SearchSuggestionsPagingSource()


    suspend fun waitingList()=service.waitingMember()

    suspend fun sendEvent(impression:Impression)=service.sendEvent(impression)

    suspend fun requestUploadRoomRecording( record:Records)=service.requestUploadRoomRecording(record)

    suspend fun createGuestUser()=service.createGuestUser()

}