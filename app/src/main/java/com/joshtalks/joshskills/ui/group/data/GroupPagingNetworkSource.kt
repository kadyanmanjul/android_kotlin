package com.joshtalks.joshskills.ui.group.data

import android.util.Log
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.flurry.sdk.it
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.ui.group.model.GroupItemData
import java.lang.Exception

private const val TAG = "GroupPagingNetworkSourc"
class GroupPagingNetworkSource(val query: String = "", val apiService: GroupApiService, val onDataLoaded : ((Boolean) -> Unit)? = null) : PagingSource<Int, GroupItemData>() {
    override fun getRefreshKey(state: PagingState<Int, GroupItemData>): Int? {
        Log.d(TAG, "getRefreshKey: ")
        return state.anchorPosition?.let { anchorPosition ->
            val anchorPage = state.closestPageToPosition(anchorPosition)
            anchorPage?.prevKey?.plus(1) ?: anchorPage?.nextKey?.minus(1)
        }
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, GroupItemData> {
        return try {

            val currentPageNo = params.key ?: 1
            val responseData = apiService.searchGroup(currentPageNo, query)

            val data = responseData.groups?.map {
                it as GroupItemData
            }

            if(currentPageNo == 1 && data.isNullOrEmpty())
                onDataLoaded?.invoke(false)
            else
                onDataLoaded?.invoke(true)

            LoadResult.Page(data!!, if(currentPageNo == 1) null else currentPageNo-1, if(data.isEmpty()) null else currentPageNo+1)
        } catch (e : Exception) {
            e.printStackTrace()
            LoadResult.Error(e)
        }
    }
}