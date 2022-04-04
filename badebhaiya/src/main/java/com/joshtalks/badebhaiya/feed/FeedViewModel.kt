package com.joshtalks.badebhaiya.feed

import androidx.databinding.ObservableBoolean
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.joshtalks.badebhaiya.core.showToast
import com.joshtalks.badebhaiya.feed.adapter.FeedAdapter
import com.joshtalks.badebhaiya.feed.model.ConversationRoomType
import com.joshtalks.badebhaiya.feed.model.RoomListResponseItem
import com.joshtalks.badebhaiya.liveroom.bottomsheet.CreateRoom
import com.joshtalks.badebhaiya.profile.request.ReminderRequest
import com.joshtalks.badebhaiya.repository.ConversationRoomRepository
import com.joshtalks.badebhaiya.repository.model.ConversationRoomRequest
import com.joshtalks.badebhaiya.repository.model.User
import kotlinx.coroutines.launch

class FeedViewModel : ViewModel() {

    val isRoomsAvailable = ObservableBoolean(true)
    val isLoading = ObservableBoolean(false)
    val isBadeBhaiyaSpeaker = ObservableBoolean(false)

    val feedAdapter = FeedAdapter()

    val repository = ConversationRoomRepository()

    fun onProfileClicked() {

    }

    fun createRoom(topic: String, callback: CreateRoom.CreateRoomCallback) {
        viewModelScope.launch {
            try {
                isLoading.set(true)
                val response = repository.createRoom(
                    ConversationRoomRequest(
                        userId = User.getInstance().userId,
                        topic = topic
                    )
                )
                if (response.isSuccessful) {
                    showToast("Room created successfully")
                    callback.onRoomCreated(response.body()!!)
                } else callback.onError("An error occurred!")
            } catch (e: Exception) {
                callback.onError(e.localizedMessage)
                showToast("Error while creating room")
            } finally {
                isLoading.set(false)
            }
        }
    }

    fun getRooms() {
        viewModelScope.launch {
            try {
                isLoading.set(true)
                isRoomsAvailable.set(true)
                val res = repository.getRoomList()
                if (res.isSuccessful) {
                    res.body()?.let {
                        val list = mutableListOf<RoomListResponseItem>()
                        if (it.liveRoomList.isNullOrEmpty().not())
                            list.addAll(it.liveRoomList!!.map { roomListResponseItem ->
                                roomListResponseItem.conversationRoomType =
                                    ConversationRoomType.LIVE
                                roomListResponseItem
                            })
                        if (it.scheduledRoomList.isNullOrEmpty().not())
                            list.addAll(it.scheduledRoomList!!.map { roomListResponseItem ->
                                roomListResponseItem.conversationRoomType =
                                    if (roomListResponseItem.isScheduled == true)
                                        ConversationRoomType.SCHEDULED
                                    else
                                        ConversationRoomType.NOT_SCHEDULED
                                roomListResponseItem
                            })
                        if (list.isNullOrEmpty())
                            isRoomsAvailable.set(false)
                        else {
                            isRoomsAvailable.set(true)
                            feedAdapter.submitList(list)
                        }
                    }
                } else {
                    isRoomsAvailable.set(false)
                    feedAdapter.submitList(null)
                }
            } catch (ex: Exception) {
                feedAdapter.submitList(null)
                isRoomsAvailable.set(false)
                ex.printStackTrace()
            } finally {
                isLoading.set(false)
            }
        }
    }

    fun setReminder(reminderRequest: ReminderRequest) {
        viewModelScope.launch {
            try {
                isLoading.set(true)
                val res = repository.setReminder(reminderRequest)
                if (res.isSuccessful && res.code() == 201) {
                    showToast("Reminder set successfully")
                } else showToast("Error while setting reminder")
            } catch (ex: Exception) {
                ex.printStackTrace()
                showToast("Error while setting reminder")
            } finally {
                isLoading.set(false)
            }
        }
    }
}
