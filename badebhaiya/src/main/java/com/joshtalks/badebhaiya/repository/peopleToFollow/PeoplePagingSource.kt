package com.joshtalks.badebhaiya.repository.peopleToFollow

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.joshtalks.badebhaiya.feed.model.Users
import com.joshtalks.badebhaiya.repository.service.RetrofitInstance

class PeoplePagingSource(
) : PagingSource<Int, Users>() {
    private val service = RetrofitInstance.signUpNetworkService

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Users> {

        // Retrofit calls that return the body type throw either IOException for network
        // failures, or HttpException for any non-2xx HTTP status codes. This code reports all
        // errors to the UI, but you can inspect/wrap the exceptions to provide more context.
        return try {
            // Key may be null during a refresh, if no explicit key is passed into Pager
            // construction. Use 0 as default, because our API is indexed started at index 0
            val pageNumber = params.key ?: 1

            // Suspending network load via Retrofit. This doesn't need to be wrapped in a
            // withContext(Dispatcher.IO) { ... } block since Retrofit's Coroutine
            // CallAdapter dispatches on a worker thread.
            val response = service.speakersList(pageNumber).body()

            // Since 0 is the lowest page number, return null to signify no more pages should
            // be loaded before it.
            val prevKey = if (pageNumber > 1) pageNumber - 1 else null

            // This API defines that it's out of data when a page returns empty. When out of
            // data, we return `null` to signify no more pages should be loaded
            val nextKey = if (!response?.speakers.isNullOrEmpty()) pageNumber + 1 else null
            LoadResult.Page(
                data = response!!.speakers,
                prevKey = prevKey,
                nextKey = nextKey
            )
        } catch (e: Exception) {
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