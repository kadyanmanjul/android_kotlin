package com.joshtalks.joshskills.ui.voip.analytics.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "voip_analytics")
data class VoipAnalyticsEntity(
    val event: String,
    val agoraCallId: String,
    val agoraMentorUid: String,
    val timeStamp: String
) {
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0
}