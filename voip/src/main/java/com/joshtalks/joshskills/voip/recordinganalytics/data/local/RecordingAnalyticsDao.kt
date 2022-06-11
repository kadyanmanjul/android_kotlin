package com.joshtalks.joshskills.voip.recordinganalytics.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface RecordingAnalyticsDao {
    @Insert
    suspend fun saveAnalytics(data: RecordingAnalyticsEntity)

    @Transaction
    @Query("SELECT * from recording_analytics WHERE is_sync= 0")
    suspend fun getUnsyncRecording(): List<RecordingAnalyticsEntity>

    @Query("UPDATE `recording_analytics` SET is_sync = 1 , server_path = :url WHERE id = :id")
    suspend fun updateSyncStatus(id: Long, url: String)
}