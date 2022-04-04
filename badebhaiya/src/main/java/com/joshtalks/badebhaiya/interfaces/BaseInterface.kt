package com.joshtalks.badebhaiya.interfaces

import com.joshtalks.badebhaiya.feed.model.RoomListResponseItem

interface ConversationRoomListAction {
    fun onRoomClick(item: RoomListResponseItem)
}