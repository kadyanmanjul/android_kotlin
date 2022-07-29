package com.joshtalks.badebhaiya.core.models

//sealed class PendingPilotAction {
//    data class Follow(val pilotUserId: String): PendingPilotAction()
//    data class SetReminder(val roomId: Int, val pilotUserId: String): PendingPilotAction()
//}

enum class PendingPilotEvent {
    FOLLOW,
    REQUEST_ROOM,
    SET_REMINDER,
}

data class PendingPilotEventData(
    val roomId: Int? = null,
    val pilotUserId: String
)