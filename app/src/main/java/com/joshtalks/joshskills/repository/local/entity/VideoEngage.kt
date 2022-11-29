package com.joshtalks.joshskills.repository.local.entity

import android.os.Parcelable
import androidx.room.*
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import com.joshtalks.joshskills.repository.local.ConvectorForGraph
import com.joshtalks.joshskills.repository.server.engage.Graph
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(tableName = "video_watch_table")
data class VideoEngage(
    @TypeConverters(
        ConvectorForGraph::class
    )
    @ColumnInfo
    @SerializedName("graph")
    var graph: List<Graph>,

    @SerializedName("video_id")
    val videoId: Int = 0,

    @SerializedName("watch_time")
    var watchTime: Long = 0,
    @Expose
    @ColumnInfo(name = "course_id")
    var courseID: Int = -1


) : Parcelable {
    @PrimaryKey(autoGenerate = true)
    @Expose
    var id: Long = 0

    @SerializedName("mentor_id")
    var mentorId: String? = null

    @SerializedName("gaid_id")
    var gID: String? = null


    @Expose
    @ColumnInfo(name = "is_sync")
    var isSync: Boolean = false

    @Expose
    @ColumnInfo(name = "is_sharable_video")
    var isSharableVideo: Boolean = false
}

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

@Parcelize
data class VideoEngageEntity constructor(
    @ColumnInfo(name = "total_time")
    var totalTime: Long? = -1,
    @ColumnInfo(name = "course_id")
    var courseId: Int? = -1,


    ) : Parcelable