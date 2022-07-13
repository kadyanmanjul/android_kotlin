package com.joshtalks.badebhaiya.repository
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.joshtalks.badebhaiya.feed.model.Fans
import com.joshtalks.badebhaiya.feed.model.Users
import com.joshtalks.badebhaiya.repository.service.RetrofitInstance

class FollowingPagingSource(val userId: String) : PagingSource<Int, Users>() {
    private val service = RetrofitInstance.profileNetworkService

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Users> {

        return try {
            val pageNumber = params.key ?: 1
            val response = service.followingList(userId,pageNumber).body()
            val prevKey = if (pageNumber > 1) pageNumber - 1 else null
            val nextKey = if (!response?.following_data.isNullOrEmpty()) pageNumber + 1 else null
            LoadResult.Page(
                data = response!!.following_data,
                prevKey = prevKey,
                nextKey = nextKey
            )
        }catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, Users>): Int? {
        return state.anchorPosition?.let {
            state.closestPageToPosition(it)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(it)?.nextKey?.minus(1)
        }
    }
}