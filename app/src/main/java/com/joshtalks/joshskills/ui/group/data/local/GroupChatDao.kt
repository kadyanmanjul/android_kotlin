package com.joshtalks.joshskills.ui.group.data.local

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.joshtalks.joshskills.ui.group.model.ChatItem

@Dao
interface GroupChatDao {

    @Insert
    suspend fun insertMessage(chat: ChatItem)

    @Query("SELECT * FROM group_chat_db WHERE groupId = :id ORDER BY msgTime")
    suspend fun getGroupMessage(id: String): List<ChatItem>

    @Query("DELETE FROM group_chat_db WHERE groupId = :id")
    suspend fun deleteGroupMessages(id: String)
}