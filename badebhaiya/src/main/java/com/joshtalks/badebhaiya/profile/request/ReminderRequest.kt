package com.joshtalks.badebhaiya.profile.request

import com.google.gson.annotations.SerializedName
import java.text.SimpleDateFormat
import java.util.*

class ReminderRequest(
    @SerializedName("room")
    val roomId: String,
    @SerializedName("user")
    val userId: String,
    @SerializedName("reminder_time")
    val reminderTime: String
) {
    constructor(roomId: String, userId: String, reminderTime: Long) :
            this(roomId, userId, SimpleDateFormat("yyyy-MM-dd hh:mm").format(Date(reminderTime)))
}