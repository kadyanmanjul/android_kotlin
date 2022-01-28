package com.joshtalks.joshskills.quizgame.analytics.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface GameAnalyticsDao {
    @Insert
    suspend fun saveAnalytics(data: GameAnalyticsEntity)

    @Transaction
    @Query("SELECT * from game_analytics")
    suspend fun getAnalytics(): List<GameAnalyticsEntity>

    @Transaction
    @Query("DELETE from game_analytics WHERE id =:id")
    suspend fun deleteAnalytics(id: Long)
}