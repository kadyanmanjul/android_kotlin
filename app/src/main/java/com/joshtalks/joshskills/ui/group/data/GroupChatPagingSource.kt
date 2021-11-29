package com.joshtalks.joshskills.ui.group.data

import android.util.Log
import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingSource
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import com.flurry.sdk.it
import com.joshtalks.joshskills.repository.local.AppDatabase
import com.joshtalks.joshskills.ui.group.lib.ChatService
import com.joshtalks.joshskills.ui.group.lib.PubNubService
import com.joshtalks.joshskills.ui.group.model.ChatItem
import java.io.IOException
import retrofit2.HttpException

private const val TAG = "GroupChatPagingSource"
@ExperimentalPagingApi
class GroupChatPagingSource(val apiService: GroupApiService, val channelId: String, val database: AppDatabase) :
    RemoteMediator<Int, ChatItem>() {
    private val chatService : ChatService = PubNubService

    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, ChatItem>
    ): MediatorResult {
        return try {
                var messages = listOf<ChatItem>()
            Log.d(TAG, "load: $loadType  ... ${state}")
                if(loadType == LoadType.APPEND) {
                    Log.d(TAG, "load: PREPEND $loadType")
                    var lastMessageTime = database.groupChatDao().getLastMessageTime(groupId = channelId)

                    messages = chatService.getMessageHistory(channelId, timeToken = lastMessageTime)

                    database.groupChatDao().insertMessages(messages)
                    Log.d(TAG, "load: $loadType")
                }
            MediatorResult.Success(
                endOfPaginationReached = if(loadType == LoadType.REFRESH) false else messages.isEmpty()
            )
        } catch (e: IOException) {
            MediatorResult.Error(e)
        } catch (e: HttpException) {
            MediatorResult.Error(e)
        }
    }
}