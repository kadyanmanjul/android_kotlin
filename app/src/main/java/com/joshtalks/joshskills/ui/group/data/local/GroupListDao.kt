package com.joshtalks.joshskills.ui.group.data.local

import androidx.lifecycle.LiveData
import androidx.paging.PagingSource
import androidx.room.*
import com.joshtalks.joshskills.ui.group.model.GroupItemData
import com.joshtalks.joshskills.ui.group.model.GroupsItem
import com.joshtalks.joshskills.ui.group.model.PageInfo

@Dao
interface GroupListDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroupItem(items: GroupsItem)

    @Query("SELECT * FROM group_list_table ORDER BY lastMsgTime DESC")
    fun getPagedGroupList() : PagingSource<Int, GroupsItem>

    @Query("SELECT groupId FROM group_list_table")
    suspend fun getGroupIds() : List<String>

    @Query("SELECT count(groupId) FROM group_list_table")
    suspend fun getGroupsCount() : Int

//    @Query("UPDATE group_list_table SET lastMessage = :lastMsg, lastMsgTime = :lastMsgTime, unreadCount = :unreadCount WHERE groupId = :id")
//    suspend fun updateGroupItem(id: String, lastMsg: String, lastMsgTime: String, unreadCount: String)

    @Update
    suspend fun updateGroupItem(item: GroupsItem)

    @Query("DELETE FROM group_list_table")
    suspend fun deleteAllGroupItems()
}