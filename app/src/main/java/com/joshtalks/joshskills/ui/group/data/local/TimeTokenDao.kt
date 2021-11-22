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

    @Query("SELECT * FROM time_token_db")
    suspend fun getAllTimeTokens()
}