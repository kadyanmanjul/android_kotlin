package com.joshtalks.joshskills.ui.group.data.local

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.joshtalks.joshskills.ui.group.model.ChatItem

@Dao
interface GroupChatDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(chat: ChatItem)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(chats: List<ChatItem>)

    @Query("SELECT * FROM group_chat_db WHERE groupId = :id ORDER BY msgTime DESC")
    suspend fun getGroupMessage(id: String): List<ChatItem>

    @Query("SELECT count(messageId) FROM group_chat_db WHERE groupId = :groupId")
    suspend fun getChatCount(groupId: String): Int

    @Query("SELECT msgTime FROM group_chat_db WHERE groupId = :groupId ORDER BY msgTime ASC limit 1")
    suspend fun getLastMessageTime(groupId: String): Long?

    @Query("SELECT msgTime FROM group_chat_db WHERE groupId = :groupId ORDER BY msgTime DESC limit 1")
    suspend fun getRecentMessageTime(groupId: String): Long?

    @Query("DELETE FROM group_chat_db WHERE groupId = :id")
    suspend fun deleteGroupMessages(id: String)

    @Query("UPDATE group_chat_db SET message = '0 Unread Messages' WHERE messageId LIKE 'unread%' AND groupId = :id")
    suspend fun resetUnreadLabel(id: String)

    @Query("SELECT msgTime FROM group_chat_db WHERE groupId = :id ORDER BY msgTime LIMIT :count-1, 1")
    suspend fun getUnreadLabelTime(count: Int, id: String): Long

    @Query("UPDATE group_chat_db SET message = :count + ' Unread Messages', msgTime = :time WHERE messageId LIKE 'unread%' AND groupId = :id")
    suspend fun setUnreadLabelTime(count: Int, time: Long, id: String)

    @Query("SELECT * FROM group_chat_db WHERE groupId = :id ORDER BY msgTime DESC")
    fun getPagedGroupChat(id: String): PagingSource<Int, ChatItem>
}