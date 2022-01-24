package com.joshtalks.joshskills.ui.lesson.conversationRoom.liveRooms

public enum class PubNubEvent(action: String) {
    CREATE_ROOM("CREATE_ROOM"),
    JOIN_ROOM("JOIN_ROOM"),
    LEAVE_ROOM("LEAVE_ROOM"),
    END_ROOM("END_ROOM"),
    IS_HAND_RAISED("IS_HAND_RAISED"),
    INVITE_SPEAKER("INVITE_SPEAKER"),
    MOVE_TO_SPEAKER("MOVE_TO_SPEAKER"),
    MOVE_TO_AUDIENCE("MOVE_TO_AUDIENCE"),
    MIC_STATUS_CHANGES("MIC_STATUS_CHANGES")
}