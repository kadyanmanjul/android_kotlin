package com.joshtalks.joshskills.ui.group.data

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingSource
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import com.flurry.sdk.it
import com.joshtalks.joshskills.ui.group.model.ChatItem

//@ExperimentalPagingApi
//class GroupChatPagingSource(val apiService: GroupApiService, val channelId: String) :
//    RemoteMediator<Long, ChatItem>() {
//    override suspend fun load(
//        loadType: LoadType,
//        state: PagingState<Long, ChatItem>
//    ): MediatorResult {
//        state.closestItemToPosition(state.anchorPosition ?: 0)
//    }
//}