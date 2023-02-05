package com.joshtalks.joshskills.premium.ui.callWithExpert.adapter

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.joshtalks.joshskills.premium.repository.local.model.Mentor
import com.joshtalks.joshskills.premium.repository.service.CommonNetworkService
import com.joshtalks.joshskills.premium.ui.callWithExpert.constant.INITIAL_PAGE_INDEX
import com.joshtalks.joshskills.premium.ui.callWithExpert.model.WalletLogs

class WalletLogsPagingSource(val commonNetworkService: CommonNetworkService): PagingSource<Int, WalletLogs>() {
    override fun getRefreshKey(state: PagingState<Int, WalletLogs>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchorPosition)?.nextKey?.minus(1)
        }
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, WalletLogs> {
        val pageIndex = params.key ?: INITIAL_PAGE_INDEX

        return try {
            val response = commonNetworkService.getPaymentTransactions(Mentor.getInstance().getId(),
                page = pageIndex
            )

            val payments = response.body()!!.payments

            val nextKey =
                if (payments.isEmpty()) {
                    null
                } else {
                    pageIndex + 1
                }
            LoadResult.Page(
                data = payments,
                prevKey = if (pageIndex == INITIAL_PAGE_INDEX) null else pageIndex,
                nextKey = nextKey
            )
        } catch (exception: Exception) {
            return LoadResult.Error(exception)
        }

    }

}