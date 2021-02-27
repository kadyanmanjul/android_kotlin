package com.joshtalks.joshskills.repository.local.entity.leaderboard

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query

@Entity
data class RecentSearch(
    @PrimaryKey
    @ColumnInfo(name = "keyword")
    var keyword: String,
)

@Dao
interface RecentSearchDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSearch(item: RecentSearch): Long

    @Query("SELECT * FROM RecentSearch")
    suspend fun getRecentSearchHistory(): List<RecentSearch>
}