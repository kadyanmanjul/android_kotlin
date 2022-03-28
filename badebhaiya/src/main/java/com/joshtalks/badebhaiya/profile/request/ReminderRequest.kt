package com.joshtalks.badebhaiya.profile.request

import com.google.gson.annotations.SerializedName

class ReminderRequest(
    @SerializedName("room")
    val room: String,
    @SerializedName("user")
    val userId: String,
    @SerializedName("reminder_time")
    val reminderTime: String
)