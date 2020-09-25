package com.joshtalks.joshskills.repository.local.entity

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.TypeConverters
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import com.joshtalks.joshskills.repository.local.ConvectorForGraph
import com.joshtalks.joshskills.repository.server.engage.Graph
import kotlinx.android.parcel.Parcelize

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

    ) : Parcelable {
    @PrimaryKey(autoGenerate = true)
    @Expose
    var id: Long = 0

    @SerializedName("mentor_id")
    var mentorId: String? = null

    @SerializedName("gaid_id")
    var gID: String? = null

    @Expose
    @ColumnInfo(name = "course_id")
    var courseID: Int = -1

    @Expose
    @ColumnInfo(name = "is_sync")
    var isSync: Boolean = false
}

@Dao
interface VideoEngageDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVideoEngage(videoEngage: VideoEngage)

    @Query(value = "SELECT * from video_watch_table where is_sync=0")
    suspend fun getAllUnSyncVideo(): List<VideoEngage>

    @Query(value = "SELECT SUM(watchTime) as abcd from video_watch_table GROUP BY course_id")
    suspend fun getWatchTime() : List<VideoEngageEntity>

    @Query("UPDATE video_watch_table SET is_sync =1 where id in (:idList)")
    suspend fun updateVideoSyncStatus(idList: List<Long>)

}

@Parcelize
data class VideoEngageEntity constructor(
    @ColumnInfo(name = "course_id")
    var courseId: Int? = -1
) : Parcelable