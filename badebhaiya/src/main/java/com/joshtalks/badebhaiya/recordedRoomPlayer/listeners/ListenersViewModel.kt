package com.joshtalks.badebhaiya.recordedRoomPlayer.listeners

import androidx.lifecycle.ViewModel
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.joshtalks.badebhaiya.feed.model.Fans
import com.joshtalks.badebhaiya.recordedRoomPlayer.listeners.model.RecordedRoomListenerItem
import com.joshtalks.badebhaiya.repository.BBRepository
import com.joshtalks.badebhaiya.repository.CommonRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

@HiltViewModel
class ListenersViewModel @Inject constructor(): ViewModel() {
    val commonRepository by lazy {
        CommonRepository()
    }

    fun listenersList(roomId: Int) = Pager(
        config = PagingConfig(pageSize = 12, enablePlaceholders = false),
        pagingSourceFactory = { commonRepository.getRecordedRoomListeners(roomId = roomId) }
    )
        .flow

}