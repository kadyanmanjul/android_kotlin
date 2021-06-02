package com.joshtalks.joshskills.conversationRoom.roomsListing

class ConversationRoomsListingItem {
    var id: Int = -1
        private set

    var topicName: String? = null
        private set

    constructor() {

    }

    constructor(id: Int, topicName: String?) {
        this.id = id
        this.topicName = topicName
    }

}
