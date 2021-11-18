package com.joshtalks.joshskills.ui.group.data

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.flurry.sdk.it
import com.joshtalks.joshskills.ui.group.model.GroupChatData

//class GroupChatPagingSource(val apiService: GroupApiService, val channelId: String) :
//    PagingSource<Int, GroupChatData>() {
//    override fun getRefreshKey(state: PagingState<Int, GroupChatData>): Int? {
//        TODO("Not yet implemented")
//    }
//
//    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, GroupChatData> {
//
//        val currentPageNo = params.key ?: 1
//        val responseData = apiService.getGroupChat(currentPageNo, channelId)
//
//        val data = responseData.chats?.map {
//            it as GroupChatData
//        }
//
//        return LoadResult.Page(data!!, if(currentPageNo == 1) null else currentPageNo-1, if(data.isEmpty()) null else currentPageNo+1)
//    }
//
//}