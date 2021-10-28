package com.joshtalks.joshskills.ui.group.analytics.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.joshtalks.joshskills.ui.voip.analytics.data.local.VoipAnalyticsEntity

@Dao
interface GroupsAnalyticsDao {
    @Insert
    suspend fun saveAnalytics(data: GroupsAnalyticsEntity)

    @Transaction
    @Query("SELECT * from groups_analytics")
    suspend fun getAnalytics(): List<GroupsAnalyticsEntity>

    @Transaction
    @Query("DELETE from groups_analytics WHERE id =:id")
    suspend fun deleteAnalytics(id: Long)
}