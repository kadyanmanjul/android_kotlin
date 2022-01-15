package com.joshtalks.joshskills.ui.group.data.local

import androidx.paging.PagingSource
import androidx.room.*
import com.joshtalks.joshskills.ui.group.model.GroupsItem

@Dao
interface GroupListDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroupItem(items: GroupsItem)

    @Query("SELECT * FROM group_list_table ORDER BY lastMsgTime DESC")
    fun getPagedGroupList(): PagingSource<Int, GroupsItem>

    @Query("SELECT * FROM group_list_table ORDER BY lastMsgTime DESC")
    fun getGroupListLocal(): List<GroupsItem>

    @Query("SELECT groupId FROM group_list_table")
    suspend fun getGroupIds(): List<String>

    @Query("SELECT * FROM group_list_table WHERE groupId = :id")
    suspend fun getGroupItem(id: String): GroupsItem

    @Query("SELECT name FROM group_list_table WHERE groupId = :id")
    suspend fun getGroupName(id: String): String

    @Query("SELECT count(groupId) FROM group_list_table")
    suspend fun getGroupsCount(): Int

    @Query("UPDATE group_list_table SET unreadCount = unreadCount+1, lastMessage = :lastMessage, lastMsgTime = :lastMsgTime WHERE groupId = :id")
    suspend fun updateGroupItem(id: String, lastMessage: String, lastMsgTime: Long): Int

    @Query("UPDATE group_list_table SET name = :groupName, groupIcon = :icon WHERE groupId = :id")
    suspend fun updateEditedGroup(id: String, groupName: String, icon: String)

    @Query("UPDATE group_list_table SET name = :groupName WHERE groupId = :id")
    suspend fun updateGroupName(id: String, groupName: String)

    @Query("UPDATE group_list_table SET unreadCount = 0 WHERE groupId = :id")
    suspend fun resetUnreadCount(id: String)

    @Query("DELETE FROM group_list_table WHERE groupId = :id")
    suspend fun deleteGroupItem(id: String)

    @Query("DELETE FROM group_list_table")
    suspend fun deleteAllGroupItems()
}