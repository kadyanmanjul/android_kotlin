package com.joshtalks.joshskills.base.storage.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface AppUsageDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertIntoAppUsage(obj: AppUsageModel)

    @Query(value = "SELECT * FROM app_usage ")
    suspend fun getAllSession(): List<AppUsageModel>

    @Query("DELETE FROM app_usage")
    suspend fun deleteAllSyncSession()
}