package com.joshtalks.joshskills.common.core.notification.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notification_event_table")
data class NotificationEvent(
    val id: String,
    val platform: String? = null,
    val time_stamp: Long = System.currentTimeMillis(),
    val action: String,
    val analytics_sent : Boolean = false
) {
    @PrimaryKey(autoGenerate = true)
    var notificationId: Int = 0
}