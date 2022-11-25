package com.joshtalks.joshskills.common.core.firestore

import com.google.gson.annotations.SerializedName
import com.joshtalks.joshskills.common.repository.local.model.Mentor

data class NotificationAnalyticsRequest(
    val id: String,
    val timestamp: Long,
    val action : String,
    val channel :String?,
    @SerializedName("mentor_id")
    val mentorId: String = Mentor.getInstance().getId()
)