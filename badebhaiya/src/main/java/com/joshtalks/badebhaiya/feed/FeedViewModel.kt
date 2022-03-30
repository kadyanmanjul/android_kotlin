package com.joshtalks.badebhaiya.feed

import android.view.View
import androidx.databinding.ObservableBoolean
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.joshtalks.badebhaiya.feed.adapter.FeedAdapter
import com.joshtalks.badebhaiya.feed.model.RoomListResponseItem
import kotlinx.coroutines.launch

class FeedViewModel: ViewModel() {

    val isRoomsAvailable = ObservableBoolean(false)
    val isBadeBhaiyaSpeaker = ObservableBoolean(false)
    val adapter = FeedAdapter()

    val onFeedItemClicked : (RoomListResponseItem, View) -> Unit = { it, view ->

    }

    fun onProfileClicked() {

    }

    fun startRoom() {

    }

    fun getRooms(userId: String) {
        viewModelScope.launch {
            try {

            } catch(ex: Exception) {

            }
        }
    }
}
