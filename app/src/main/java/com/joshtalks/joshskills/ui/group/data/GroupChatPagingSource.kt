package com.joshtalks.joshskills.ui.group.data

//class GroupChatPagingSource(val apiService: GroupApiService, val channelId: String) :
//    PagingSource<Int, ChatItem>() {
//    override fun getRefreshKey(state: PagingState<Int, ChatItem>): Int? {
//        TODO("Not yet implemented")
//    }
//
//    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, ChatItem> {
//
//        val currentPageNo = params.key ?: 1
//        val responseData = apiService.getGroupChat(currentPageNo, channelId)
//
//        val data = responseData.chats?.map {
//            it as ChatItem
//        }
//
//        return LoadResult.Page(data!!, if(currentPageNo == 1) null else currentPageNo-1, if(data.isEmpty()) null else currentPageNo+1)
//    }
//
//}