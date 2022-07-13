package com.joshtalks.badebhaiya.repository
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.joshtalks.badebhaiya.feed.model.Fans
import com.joshtalks.badebhaiya.repository.service.RetrofitInstance

class FansPagingSource(
) : PagingSource<Int, Fans>() {
    private val service = RetrofitInstance.profileNetworkService

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Fans> {

        return try {
            val pageNumber = params.key ?: 1
            val response = service.fansList(pageNumber).body()
            val prevKey = if (pageNumber > 1) pageNumber - 1 else null
            val nextKey = if (!response?.follower_data.isNullOrEmpty()) pageNumber + 1 else null
            LoadResult.Page(
                data = response!!.follower_data,
                prevKey = prevKey,
                nextKey = nextKey
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, Fans>): Int? {
        return state.anchorPosition?.let {
            state.closestPageToPosition(it)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(it)?.nextKey?.minus(1)
        }
    }
}