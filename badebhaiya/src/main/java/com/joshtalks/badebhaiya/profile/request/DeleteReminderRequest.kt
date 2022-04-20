package com.joshtalks.badebhaiya.profile.request

import com.google.gson.annotations.SerializedName
import java.text.SimpleDateFormat
import java.util.*

class DeleteReminderRequest(
    @SerializedName("room")
    val roomId: String,
    @SerializedName("user")
    val userId: String
) {

}