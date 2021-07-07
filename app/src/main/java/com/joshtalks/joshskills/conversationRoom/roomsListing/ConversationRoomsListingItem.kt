package com.joshtalks.joshskills.conversationRoom.roomsListing

class ConversationRoomsListingItem {
    var channel_name: String = ""
        private set

    var topic: String? = null
        private set

    var users: ArrayList<ConversationRoomSpeakerList>? = null

    var started_by: Int? = -1
        private set

    var room_id: Int? = null

    constructor() {

    }

    constructor(channelName: String, topic: String?) {
        this.channel_name = channelName
        this.topic = topic
    }

    constructor(channel_name: String, topic: String?, started_by: Int?, room_id: Int?) {
        this.channel_name = channel_name
        this.topic = topic
        this.started_by = started_by
        this.room_id = room_id
    }

    fun addUser(item: ConversationRoomSpeakerList){
        if (users == null) {
            users = arrayListOf()
        }
        users!!.add(item)
    }
}

class ConversationRoomSpeakerList {

    var is_hand_raised: Boolean = false
        private set
    var is_mic_on: Boolean = false
        private set
    var is_moderator: Boolean = false
        private set
    private var is_speaker: Boolean = false

    var mentor_id: String = ""
        private set
    var name: String = ""
        private set
    var photo_url: String? = null
        private set

    constructor() {

    }

    constructor(
        is_hand_raised: Boolean,
        is_mic_on: Boolean,
        is_moderator: Boolean,
        is_speaker: Boolean,
        mentor_id: String,
        name: String,
        photo_url: String?
    ) {
        this.is_hand_raised = is_hand_raised
        this.is_mic_on = is_mic_on
        this.is_moderator = is_moderator
        this.is_speaker = is_speaker
        this.mentor_id = mentor_id
        this.name = name
        this.photo_url = photo_url
    }

    fun isUserSpeaker(): Boolean{
        return is_speaker
    }
}
