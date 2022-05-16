package com.joshtalks.joshskills.core.firestore

data class NotificationAnalyticsRequest(
    val id: String,
    val timestamp: Long,
    val action : String,
    val channel :String
)