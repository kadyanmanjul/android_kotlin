package com.joshtalks.joshskills.buypage.new_buy_page_layout.utils

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.joshtalks.joshskills.common.repository.service.CommonNetworkService
import com.joshtalks.joshskills.buypage.new_buy_page_layout.model.ReviewItem
import com.joshtalks.joshskills.buypage.new_buy_page_layout.repo.BuyPageService

class ReviewPagingSource(val testId: Int, val apiService: BuyPageService) : PagingSource<Int, ReviewItem>() {
    override fun getRefreshKey(state: PagingState<Int, ReviewItem>): Int? {
        return state.anchorPosition?.let { position ->
            val anchorPage = state.closestPageToPosition(position)
            anchorPage?.prevKey?.plus(1) ?: anchorPage?.nextKey?.minus(1)
        }
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, ReviewItem> {
        return try {
            val currentPageNo = params.key ?: 1
            val data = apiService.getReviews(currentPageNo, testId).reviews

            LoadResult.Page(
                data,
                if (currentPageNo == 1) null else currentPageNo - 1,
                if (data.isEmpty()) null else currentPageNo + 1
            )
        } catch (e: Exception) {
            e.printStackTrace()
            LoadResult.Error(e)
        }
    }
}