package com.joshtalks.joshskills.core.notification.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "schedule_notification")
data class ScheduleNotification(

    @PrimaryKey
    val id: String,
    val category: String,
    val title: String,
    val body: String,
    val execute_after: Long,
    val action: String,
    val action_data: String,
    val is_scheduled: Boolean = false,
    val is_shown: Boolean = false,
    val is_event_sent: Boolean = false
)