package com.joshtalks.joshskills.conversationRoom.liveRooms

sealed class ConversationLiveRoomNavigation {
    object ApiCallError : ConversationLiveRoomNavigation()
    object ExitRoom : ConversationLiveRoomNavigation()

}
