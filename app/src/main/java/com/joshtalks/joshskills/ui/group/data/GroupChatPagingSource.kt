package com.joshtalks.joshskills.ui.group.data

import android.util.Log
import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import com.joshtalks.joshskills.repository.local.AppDatabase
import com.joshtalks.joshskills.ui.group.lib.ChatService
import com.joshtalks.joshskills.ui.group.lib.PubNubService
import com.joshtalks.joshskills.ui.group.model.ChatItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
            val messages = mutableListOf<ChatItem>()
            Log.d(TAG, "load: $loadType  ... ${state}")
            when (loadType) {
                // Getting Older Messages
                LoadType.APPEND -> {
                    Log.d(TAG, "load: APPEND $loadType")
                    val lastMessageTime = database.groupChatDao().getLastMessageTime(groupId = channelId)

                    withContext(Dispatchers.IO) {
                        messages.addAll(chatService.getMessageHistory(channelId, startTime = lastMessageTime))
                    }

                    database.groupChatDao().insertMessages(messages)
                }

                // Getting Recent Messages
                LoadType.PREPEND -> {
                    Log.d(TAG, "load: PREPEND $loadType")
                    val recentMessageTime = database.groupChatDao().getRecentMessageTime(groupId = channelId)

                    recentMessageTime?.let {
                        withContext(Dispatchers.IO) {
                            messages.addAll(chatService.getUnreadMessages(channelId, startTime = recentMessageTime))
                        }
                        database.groupChatDao().insertMessages(messages)
                    }
                }
            }
            MediatorResult.Success(
                endOfPaginationReached = if (loadType == LoadType.REFRESH) false else messages.isEmpty()
            )
        } catch (e: IOException) {
            MediatorResult.Error(e)
        } catch (e: HttpException) {
            MediatorResult.Error(e)
        }
    }
}