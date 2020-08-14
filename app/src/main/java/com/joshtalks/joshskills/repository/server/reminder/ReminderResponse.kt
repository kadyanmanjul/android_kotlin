package com.joshtalks.joshskills.repository.server.reminder

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

data class ReminderResponse(
    @SerializedName("id")
    @Expose
    var id: Int,
    @SerializedName("mentor")
    @Expose
    var mentor: String,
    @SerializedName("reminder_frequency")
    @Expose
    var reminderFrequency: String,
    @SerializedName("status")
    @Expose
    var status: String,
    @SerializedName("reminder_time")
    @Expose
    var reminderTime: String,
    @SerializedName("created_at")
    @Expose
    var createdAt: String,
    @SerializedName("modified_at")
    @Expose
    var modifiedAt: String
) {
    var isSelected: Boolean = false
}