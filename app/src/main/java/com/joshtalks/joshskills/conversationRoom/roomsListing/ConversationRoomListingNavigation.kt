package com.joshtalks.joshskills.conversationRoom.roomsListing

sealed class ConversationRoomListingNavigation {
    class OpenConversationLiveRoom(
        val channelName: String?,
        val uid: Int?,
        val token: String?,
        val isRoomCreatedByUser: Boolean,
        val roomId: Int?,
        val startedBy: Int?
    ) : ConversationRoomListingNavigation()

    class ApiCallError(val error: String): ConversationRoomListingNavigation()

    object NoRoomAvailable: ConversationRoomListingNavigation()
    object AtleastOneRoomAvailable: ConversationRoomListingNavigation()
}
