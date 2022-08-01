package com.joshtalks.badebhaiya.repository

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.joshtalks.badebhaiya.feed.model.Fans
import com.joshtalks.badebhaiya.recordedRoomPlayer.listeners.model.RecordedRoomListenerItem
import com.joshtalks.badebhaiya.repository.service.RetrofitInstance

class ListenersPagingSource(val roomId: Int) : PagingSource<Int, RecordedRoomListenerItem>() {
    private val service = RetrofitInstance.commonNetworkService

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, RecordedRoomListenerItem> {

        return try {
            val pageNumber = params.key ?: 1
            val response = service.getRecordedRoomListeners(roomId, pageNumber).body()
            val prevKey = if (pageNumber > 1) pageNumber - 1 else null
            val nextKey = if (!response.isNullOrEmpty()) pageNumber + 1 else null
            LoadResult.Page(
                data = response!!,
                prevKey = prevKey,
                nextKey = nextKey
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, RecordedRoomListenerItem>): Int? {
        return state.anchorPosition?.let {
            state.closestPageToPosition(it)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(it)?.nextKey?.minus(1)
        }
    }
}
