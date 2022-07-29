package com.joshtalks.badebhaiya.repository

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.joshtalks.badebhaiya.feed.model.Users
import com.joshtalks.badebhaiya.feed.model.searchSuggestion.User
import com.joshtalks.badebhaiya.repository.service.RetrofitInstance


class SearchSuggestionsPagingSource(
) : PagingSource<Int, User>() {
    private val service = RetrofitInstance.conversationRoomNetworkService

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, User> {

        return try {
            val pageNumber = params.key ?: 1
            val response = service.getSearchSuggestions(pageNumber).body()

            val prevKey = if (pageNumber > 1) pageNumber - 1 else null

            val nextKey = if (!response?.users.isNullOrEmpty()) pageNumber + 1 else null
            LoadResult.Page(
                data = response!!.users,
                prevKey = prevKey,
                nextKey = nextKey
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, User>): Int? {
        return state.anchorPosition?.let {
            state.closestPageToPosition(it)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(it)?.nextKey?.minus(1)
        }
    }
}