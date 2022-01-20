package com.joshtalks.joshskills.conversationRoom.bottomsheet

data class ConversationRoomBottomSheetInfo(
    val fromModerator: Boolean,
    val fromSpeaker: Boolean,
    val toSpeaker: Boolean,
    val userName: String,
    val userPhoto: String,
    val isSelf: Boolean
)
