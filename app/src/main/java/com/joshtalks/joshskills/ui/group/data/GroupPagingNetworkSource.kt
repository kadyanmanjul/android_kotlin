package com.joshtalks.joshskills.ui.group.data

import android.util.Log
import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingSource
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import com.flurry.sdk.it
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.ui.group.lib.ChatService
import com.joshtalks.joshskills.ui.group.lib.PubNubService
import com.joshtalks.joshskills.ui.group.model.GroupItemData
import com.joshtalks.joshskills.ui.group.model.PageInfo
import com.pubnub.api.models.consumer.PNPage
import java.lang.Exception

private const val TAG = "PubNub_GroupPaging"
class GroupPagingNetworkSource(val query: String = "", val isSearching : Boolean = false, val apiService: GroupApiService, val mentorId : String = "", val onDataLoaded : ((Boolean) -> Unit)? = null) : PagingSource<PageInfo, GroupItemData>() {
    private val chatService : ChatService = PubNubService
    override fun getRefreshKey(state: PagingState<PageInfo, GroupItemData>): PageInfo? {
        return null
    }

    override suspend fun load(params: LoadParams<PageInfo>): LoadResult<PageInfo, GroupItemData> {
        return try {
            Log.d(TAG, "load: ${params.key}")
            val isFirstPage = params.key == null
            var pageInfo = PageInfo()
            val responseData = if(isSearching)
                apiService.searchGroup(if(isFirstPage) 1 else params.key?.currentPage ?: 1, query)
            else {
                val pubNubResponse = chatService.fetchGroupList(
                    if(isFirstPage) null else PageInfo(pubNubNext = params.key?.pubNubNext, pubNubPrevious = params.key?.pubNubPrevious)
                )
                pageInfo = pubNubResponse?.getPageInfo() ?: PageInfo()
                pubNubResponse?.getData()
            }

            Log.d(TAG, "load: $pageInfo")

            val data = responseData?.groups?.map {
                it as GroupItemData
            }

            if(isFirstPage && data.isNullOrEmpty())
                onDataLoaded?.invoke(false)
            else
                onDataLoaded?.invoke(true)

            val result = if(isSearching)
                LoadResult.Page(
                    data!!,
                    if(isFirstPage) null else PageInfo(currentPage = (params.key?.currentPage ?: 1)-1),
                    if(data.isEmpty()) null else PageInfo(currentPage = (params.key?.currentPage ?: 1)+1)
                )
            else
                LoadResult.Page(
                    data!!,
                    if(isFirstPage) null else PageInfo(pubNubPrevious = pageInfo.pubNubPrevious),
                    if(data.isEmpty()) null else PageInfo(pubNubNext = pageInfo.pubNubNext)
                )
            result
        } catch (e : Exception) {
            e.printStackTrace()
            LoadResult.Error(e)
        }
    }
}