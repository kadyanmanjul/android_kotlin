package com.joshtalks.joshskills.ui.group.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.joshtalks.joshskills.ui.group.model.TimeTokenRequest

@Dao
interface TimeTokenDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNewTimeToken(timeToken: TimeTokenRequest)

    @Query("UPDATE time_token_db set timeToken = :time WHERE groupId = :id")
    suspend fun updateTimeToken(id: String, time: Long)

    @Query("SELECT * FROM time_token_db WHERE groupId in (SELECT groupId FROM group_list_table)")
    suspend fun getAllTimeTokens(): List<TimeTokenRequest>

    @Query("SELECT timeToken FROM time_token_db WHERE groupId = :id")
    fun getOpenedTime(id: String): Long?

    @Query("DELETE FROM time_token_db WHERE groupId = :groupId AND timeToken = :time")
    suspend fun deleteTimeEntry(groupId: String, time: Long)

    @Query("DELETE FROM time_token_db WHERE groupId = :groupId")
    suspend fun deleteTimeToken(groupId: String)

    @Query("DELETE FROM time_token_db WHERE groupId NOT IN (SELECT groupId FROM group_list_table)")
    suspend fun deleteLeftGroups()
}