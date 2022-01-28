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

    @Query("SELECT * FROM time_token_db")
    suspend fun getAllTimeTokens(): List<TimeTokenRequest>

    @Query("SELECT timeToken FROM time_token_db WHERE groupId = :id")
    fun getOpenedTime(id: String): Long?

    @Query("DELETE FROM time_token_db WHERE groupId = :groupId AND timeToken = :time")
    suspend fun deleteTimeEntry(groupId: String, time: Long)

    @Query("DELETE FROM time_token_db WHERE groupId = :groupId")
    suspend fun deleteTimeToken(groupId: String)
}