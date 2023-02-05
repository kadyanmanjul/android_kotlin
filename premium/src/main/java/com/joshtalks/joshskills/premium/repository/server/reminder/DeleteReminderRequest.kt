package com.joshtalks.joshskills.premium.repository.server.reminder

import com.google.gson.annotations.SerializedName

data class DeleteReminderRequest(
    @SerializedName("mentor_id")
    val mentorId: String,
    @SerializedName("reminder_ids")
    val reminderId: List<Int>
)