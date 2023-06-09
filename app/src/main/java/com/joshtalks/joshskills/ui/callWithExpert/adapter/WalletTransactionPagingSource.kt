package com.joshtalks.joshskills.ui.callWithExpert.adapter

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.repository.service.CommonNetworkService
import com.joshtalks.joshskills.ui.callWithExpert.constant.INITIAL_PAGE_INDEX
import com.joshtalks.joshskills.ui.callWithExpert.model.Transaction

class WalletTransactionPagingSource(val commonNetworkService: CommonNetworkService): PagingSource<Int, Transaction>() {
    override fun getRefreshKey(state: PagingState<Int, Transaction>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchorPosition)?.nextKey?.minus(1)
        }
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Transaction> {
        val pageIndex = params.key ?: INITIAL_PAGE_INDEX

        return try {
            val response = commonNetworkService.getWalletTransactions(Mentor.getInstance().getId(),
                page = pageIndex
            )

            val transactions = response.body()!!.transactions

            val nextKey =
                if (transactions.isEmpty()) {
                    null
                } else {
                    pageIndex + 1
                }
            LoadResult.Page(
                data = transactions,
                prevKey = if (pageIndex == INITIAL_PAGE_INDEX) null else pageIndex,
                nextKey = nextKey
            )
        } catch (exception: Exception) {
            return LoadResult.Error(exception)
        }

    }

}