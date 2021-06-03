package com.joshtalks.joshskills.conversationRoom.roomsListing

sealed class ConversationRoomListingNavigation {
    class OpenConversationLiveRoom(
        val channelName: String?,
        val uid: Int?,
        val token: String?,
        val isRoomCreatedByUser: Boolean,
        val roomId: Int?
    ) : ConversationRoomListingNavigation()

    class ApiCallError(): ConversationRoomListingNavigation()
}
