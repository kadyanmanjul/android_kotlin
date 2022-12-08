package com.joshtalks.joshskills.voip.recordinganalytics.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recording_analytics")
data class RecordingAnalyticsEntity(
    @ColumnInfo(name = "agora_call")
    val agora_call: String,
    @ColumnInfo(name = "agora_mentor")
    val agora_mentor: String,
    @ColumnInfo(name = "timestamp")
    val timestamp: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "local_path")
    val localPath: String,
    @ColumnInfo(name = "server_path")
    val serverPath: String? = "",
    @ColumnInfo(name = "is_sync")
    val isSync: Boolean = false,
) {
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0
}