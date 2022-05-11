package com.joshtalks.joshskills.core.notification.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notification_table")
data class NotificationModel(

    @PrimaryKey
    val id: String,
    val platform: String,
    val time_received: Long,
    val time_shown: Long,
    val action: String,
    val analytics_sent: Boolean = false
)
