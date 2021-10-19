package com.joshtalks.joshskills.ui.group.repository

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.joshtalks.joshskills.ui.group.model.GroupItemData
import com.joshtalks.joshskills.ui.group.model.GroupModel
import java.lang.Exception
import kotlinx.coroutines.delay

class GroupPagingNetworkSource : PagingSource<Int, GroupItemData>() {
    override fun getRefreshKey(state: PagingState<Int, GroupItemData>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            val anchorPage = state.closestPageToPosition(anchorPosition)
            anchorPage?.prevKey?.plus(1) ?: anchorPage?.nextKey?.minus(1)
        }
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, GroupItemData> {
        return try {
            val nextPageNumber = params.key ?: 1
            //TODO: Do API Call
            delay(1000)
            val data = listOf<GroupItemData>(
                GroupModel(1, "Hello_${nextPageNumber}", "200 members, 14 online"),
                GroupModel(2, "Hello1_${nextPageNumber}", "201 members, 13 online"),
                GroupModel(3, "Hello2_${nextPageNumber}", "202 members, 12 online"),
                GroupModel(4, "Hello3_${nextPageNumber}", "203 members, 11 online"),
                GroupModel(5, "Hello4_${nextPageNumber}", "204 members, 10 online"),
                GroupModel(6, "Hello5_${nextPageNumber}", "205 members, 9 online"),
                GroupModel(7, "Hello6_${nextPageNumber}", "206 members, 8 online"),
            )
            LoadResult.Page(data, null, nextPageNumber+1)
        } catch (e : Exception) {
            e.printStackTrace()
            LoadResult.Error(e)
        }
    }
}