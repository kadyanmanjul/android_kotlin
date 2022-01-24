package com.joshtalks.joshskills.ui.lesson.conversationRoom.liveRooms

class ConversationLiveRoomUser {
    var name: String? = null
        private set
    var isIs_speaker = false
        private set
    var isIs_moderator = false
        private set
    var isIs_hand_raised = false
        private set
    var isIs_mic_on = false
        private set
    var photo_url: String? = null
        private set

    constructor() {}
    constructor(
        name: String?,
        is_speaker: Boolean,
        is_moderator: Boolean,
        is_hand_raised: Boolean,
        is_mic_on: Boolean,
        photo_url: String?
    ) {
        this.name = name
        isIs_speaker = is_speaker
        isIs_moderator = is_moderator
        isIs_hand_raised = is_hand_raised
        isIs_mic_on = is_mic_on
        this.photo_url = photo_url
    }
}