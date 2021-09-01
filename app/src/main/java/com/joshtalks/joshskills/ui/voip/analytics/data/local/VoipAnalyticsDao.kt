package com.joshtalks.joshskills.ui.voip.analytics.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface VoipAnalyticsDao {
    @Insert
    suspend fun saveAnalytics(data: VoipAnalyticsEntity)

    @Transaction
    @Query("SELECT * from voip_analytics")
    suspend fun getAnalytics(): List<VoipAnalyticsEntity>

    @Transaction
    @Query("DELETE from voip_analytics WHERE id =:id")
    suspend fun deleteAnalytics(id: Long)
}