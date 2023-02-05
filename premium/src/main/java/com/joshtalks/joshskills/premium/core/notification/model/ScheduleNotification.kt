package com.joshtalks.joshskills.premium.core.notification.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.joshtalks.joshskills.premium.core.notification.client_side.AlarmFrequency
import com.joshtalks.joshskills.premium.repository.local.FrequencyConverter

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
    val is_event_sent: Boolean = false,
    val is_canceled: Boolean = false,
    @TypeConverters(FrequencyConverter::class)
    var frequency: AlarmFrequency? = AlarmFrequency.ONCE
)