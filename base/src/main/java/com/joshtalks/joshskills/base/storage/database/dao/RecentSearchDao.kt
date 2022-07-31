package com.joshtalks.joshskills.base.storage.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface RecentSearchDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSearch(item: RecentSearch): Long

    @Query("SELECT * FROM RecentSearch ORDER BY timestamp DESC")
    suspend fun getRecentSearchHistory(): List<RecentSearch>

    @Query("DELETE FROM RecentSearch")
    suspend fun clearHistory()
}