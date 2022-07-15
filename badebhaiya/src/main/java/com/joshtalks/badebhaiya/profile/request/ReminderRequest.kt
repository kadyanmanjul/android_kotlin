package com.joshtalks.badebhaiya.profile.request

import com.google.gson.annotations.SerializedName
import java.text.SimpleDateFormat
import java.util.*

class ReminderRequest(
    @SerializedName("room")
    val roomId: String,
    @SerializedName("user")
    val userId: String,
    @SerializedName("from_page")
    val from:String,
    @SerializedName("reminder_time")
    val reminderTime: String,
    @SerializedName("is_from_deeplink")
    val isFromDeeplink:Boolean
) {
    constructor(roomId: String, userId: String, reminderTime: Long,isFromDeeplink: Boolean, from: String) :
            this(roomId, userId, from,SimpleDateFormat("yyyy-MM-dd hh:mm").format(Date(reminderTime)),isFromDeeplink)
}