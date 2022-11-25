package com.joshtalks.joshskills.common.repository.server.reminder

import com.google.gson.annotations.SerializedName

data class ReminderRequest(
    @SerializedName("mentor_id")
    val mentorId: String,
    @SerializedName("reminder_time")
    val reminderTime: String,
    @SerializedName("reminder_frequency")
    val reminderFrequency: String,
    @SerializedName("status")
    val status: String,
    @SerializedName("previous_reminder_time")
    val previousReminderTime: String
)