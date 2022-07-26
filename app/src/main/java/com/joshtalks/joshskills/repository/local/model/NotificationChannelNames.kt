package com.joshtalks.joshskills.repository.local.model

enum class NotificationChannelNames(val type: String) {
    CLASSES("Classes"),
    P2P("Voice Calling"),
    UPDATES("Updates"),
    GROUP_CHATS("Group Chats"),
    OTHERS("Others"),
    DEFAULT("JoshTalksDefault"),
}