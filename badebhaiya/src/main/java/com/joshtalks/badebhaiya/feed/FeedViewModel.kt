package com.joshtalks.badebhaiya.feed

import android.view.View
import androidx.databinding.ObservableBoolean
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.joshtalks.badebhaiya.core.showToast
import com.joshtalks.badebhaiya.feed.adapter.FeedAdapter
import com.joshtalks.badebhaiya.feed.model.RoomListResponseItem
import com.joshtalks.badebhaiya.liveroom.bottomsheet.CreateRoom
import com.joshtalks.badebhaiya.repository.ConversationRoomRepository
import com.joshtalks.badebhaiya.repository.model.ConversationRoomRequest
import com.joshtalks.badebhaiya.repository.model.User
import kotlinx.coroutines.launch

class FeedViewModel : ViewModel() {

    val isRoomsAvailable = ObservableBoolean(true)
    val isLoading = ObservableBoolean(false)
    val isBadeBhaiyaSpeaker = ObservableBoolean(true)

    val feedAdapter = FeedAdapter()

    val repository = ConversationRoomRepository()
    val onFeedItemClicked: (RoomListResponseItem?, View?) -> Unit = { it, view ->
        //TODO() - open room
    }

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
                val res = repository.getRoomList()
                if (res.isSuccessful) {
                    res.body()?.let {
                        if (it.liveRoomList.isNullOrEmpty())
                            isRoomsAvailable.set(false)
                        else
                            feedAdapter.submitList(it.liveRoomList)
                    }
                }
            } catch (ex: Exception) {
                isRoomsAvailable.set(false)
                ex.printStackTrace()
            } finally {
                isLoading.set(false)
            }
        }
    }
}
