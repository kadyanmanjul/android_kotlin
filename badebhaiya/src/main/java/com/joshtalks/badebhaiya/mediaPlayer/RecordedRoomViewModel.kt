package com.joshtalks.badebhaiya.mediaPlayer

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.joshtalks.badebhaiya.liveroom.LiveRoomState

class RecordedRoomViewModel : ViewModel() {
    // TODO: Implement the ViewModel

    var lvRoomState= MutableLiveData<LiveRoomState>()
}