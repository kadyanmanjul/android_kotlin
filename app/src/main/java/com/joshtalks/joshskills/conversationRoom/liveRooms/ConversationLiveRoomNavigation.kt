package com.joshtalks.joshskills.conversationRoom.liveRooms

sealed class ConversationLiveRoomNavigation {
    class ApiCallError() : ConversationLiveRoomNavigation()
    class ExitRoom() : ConversationLiveRoomNavigation()

}
