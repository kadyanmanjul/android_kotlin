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
    var watchTime: Long = 0


) : Parcelable {
    @PrimaryKey(autoGenerate = true)
    @Expose
    var id: Long = 0


    @SerializedName("mentor_id")
    var mentorId: String? = null

    @SerializedName("gaid_id")
    var gID: String? = null
}


@Dao
interface VideoEngageDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVideoEngage(videoEngage: VideoEngage)

    @Query(value = "SELECT * from video_watch_table ")
    suspend fun getAllUnSyncVideo(): List<VideoEngage>

    @Query("delete from video_watch_table where id in (:idList)")
    suspend fun deleteVideos(idList: List<Long>)


}
