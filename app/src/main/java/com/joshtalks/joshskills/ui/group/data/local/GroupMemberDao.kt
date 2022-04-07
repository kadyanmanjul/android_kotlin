package com.joshtalks.joshskills.ui.group.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy.REPLACE
import androidx.room.Query
import com.joshtalks.joshskills.ui.group.model.GroupMember

@Dao
interface GroupMemberDao {

    @Insert(onConflict = REPLACE)
    suspend fun insertMember(member: GroupMember)

    @Insert(onConflict = REPLACE)
    suspend fun insertMembers(members: List<GroupMember>)

    @Query("SELECT * FROM group_member_table WHERE groupId = :groupId ORDER by isAdmin DESC")
    suspend fun getMembersFromGroup(groupId: String): List<GroupMember>

    @Query("DELETE FROM group_member_table WHERE groupId = :groupId AND mentorID = :mentorId")
    suspend fun deleteMemberFromGroup(groupId: String, mentorId: String)

    @Query("DELETE FROM group_member_table")
    suspend fun clearMemberTable()
}