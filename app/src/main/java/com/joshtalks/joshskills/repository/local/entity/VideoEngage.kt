package com.joshtalks.joshskills.repository.local.entity

import android.os.Parcelable
import androidx.room.*
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import com.joshtalks.joshskills.base.storage.database.ConvectorForGraph
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

@Parcelize
data class VideoEngageEntity constructor(
    @ColumnInfo(name = "total_time")
    var totalTime: Long? = -1,
    @ColumnInfo(name = "course_id")
    var courseId: Int? = -1,
) : Parcelable