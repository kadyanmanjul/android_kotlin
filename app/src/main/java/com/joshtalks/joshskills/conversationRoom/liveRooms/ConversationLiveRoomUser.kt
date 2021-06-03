package com.joshtalks.joshskills.conversationRoom.liveRooms

class ConversationLiveRoomUser {

    var name: String? = null
        private set

    var is_speaker: Boolean? = false
        private set

    var photo_url: String? = ""
        private set

    constructor() {

    }

    constructor(name: String?, is_speaker: Boolean?, photo_url: String?) {
        this.name = name
        this.is_speaker = is_speaker
        this.photo_url = photo_url
    }


}
