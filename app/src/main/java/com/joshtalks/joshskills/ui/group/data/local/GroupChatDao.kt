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
    suspend fun insertMessage(chat : ChatItem)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(chats : List<ChatItem>)

    @Query("SELECT * FROM group_chat_db WHERE groupId = :id ORDER BY msgTime")
    suspend fun getGroupMessage(id: String): List<ChatItem>

    @Query("SELECT count(messageId) FROM group_chat_db WHERE groupId = :groupId")
    suspend fun getChatCount(groupId : String) : Int

    @Query("DELETE FROM group_chat_db WHERE groupId = :id")
    suspend fun deleteGroupMessages(id: String)

    @Query("SELECT * FROM group_chat_db WHERE groupId = :id ORDER BY msgTime")
    fun getPagedGroupChat(id: String) : PagingSource<Int, ChatItem>
}