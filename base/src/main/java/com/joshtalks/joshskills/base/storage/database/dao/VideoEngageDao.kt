package com.joshtalks.joshskills.base.storage.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface VideoEngageDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVideoEngage(videoEngage: VideoEngage)

    @Query(value = "SELECT * from video_watch_table where is_sync=0")
    suspend fun getAllUnSyncVideo(): List<VideoEngage>

    // @Query("SELECT sum(watchTime) from video_watch_table ")
    @Query("SELECT SUM(watchTime) as total_time,course_id FROM video_watch_table GROUP BY course_id ORDER BY total_time DESC LIMIT 1;")
    suspend fun getWatchTime(): VideoEngageEntity?

    @Query("SELECT SUM(watchTime) as total_time FROM video_watch_table")
    suspend fun getOverallWatchTime(): Long?

    @Query("SELECT * FROM video_watch_table where videoId=:videoId ORDER BY id DESC limit 1 ")
    suspend fun getWatchTimeForVideo(videoId: Int): VideoEngage?

    @Query("UPDATE video_watch_table SET is_sync =1 where id in (:idList)")
    suspend fun updateVideoSyncStatus(idList: List<Long>)
}