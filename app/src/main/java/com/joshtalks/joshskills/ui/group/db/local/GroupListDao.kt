package com.joshtalks.joshskills.ui.group.db.local

import androidx.room.*

@Dao
interface GroupListDao {

    @Insert
    suspend fun insertGroupItem(item: GroupListEntity)

    @Query("SELECT * FROM group_list_table")
    suspend fun getFullGroupList() : List<GroupListEntity>

//    @Query("UPDATE group_list_table SET lastMessage = :lastMsg, lastMsgTime = :lastMsgTime, unreadCount = :unreadCount WHERE groupId = :id")
//    suspend fun updateGroupItem(id: String, lastMsg: String, lastMsgTime: String, unreadCount: String)

    @Update
    suspend fun updateGroupItem(item: GroupListEntity)

    @Query("DELETE FROM group_list_table")
    suspend fun deleteAllGroupItems()
}