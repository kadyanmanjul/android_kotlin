package com.joshtalks.joshskills.premium.repository.local.model

enum class NotificationChannelData(val type: String, val id: String) {
    CLASSES("Classes", "Classes"),
    UPDATES("Course Updates", "109000"),
    REMINDERS("Reminders", "reminders"),
    MESSAGES_REQUESTS("Messages & Requests", "messages_requests"),
    OTHERS("Others", "josh_app_alarm_channel"),
    DEFAULT("Default Notifications", "101111"),
    DOWNLOADS("Downloads", "download_channel"),
    UPLOADS("Uploads", "FILE_UPLOAD"),
    CALLS("Calls", "10001"),
    LOCAL_NOTIFICATIONS("Local Notifications", "josh_app_local_notification_channel"),
}