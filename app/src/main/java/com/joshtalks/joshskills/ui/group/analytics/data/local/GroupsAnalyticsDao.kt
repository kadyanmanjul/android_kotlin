package com.joshtalks.joshskills.ui.group.analytics.data.local

import androidx.annotation.Nullable
import androidx.room.*
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

    @Nullable
    @Query("SELECT lastSentMsgTime FROM group_chat_analytics WHERE groupId = :id")
    suspend fun getLastSentMsgTime(id: String): Long?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setLastSentMsgTime(item: GroupChatAnalyticsEntity)
}