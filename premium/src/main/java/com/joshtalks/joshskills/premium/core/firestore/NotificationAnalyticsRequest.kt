package com.joshtalks.joshskills.premium.core.firestore

import com.google.gson.annotations.SerializedName
import com.joshtalks.joshskills.premium.repository.local.model.Mentor

data class NotificationAnalyticsRequest(
    val id: String,
    val timestamp: Long,
    val action : String,
    val channel :String?,
    @SerializedName("mentor_id")
    val mentorId: String = Mentor.getInstance().getId()
)