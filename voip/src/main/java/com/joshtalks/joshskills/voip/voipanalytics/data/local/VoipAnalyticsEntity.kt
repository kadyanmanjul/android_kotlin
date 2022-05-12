package com.joshtalks.joshskills.voip.voipanalytics.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "voip_analytics")
data class VoipAnalyticsEntity(
    @ColumnInfo(name = "type")
    val type: String,
    @ColumnInfo(name = "agora_call")
    val agora_call: String,
    @ColumnInfo(name = "agora_mentor")
    val agora_mentor: String,
    @ColumnInfo(name = "timestamp")
    val timestamp: String
) {
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0
}